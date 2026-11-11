# Book Search — Open Library demo (Android)

App Android que busca libros en el catálogo de **Open Library** y muestra su detalle. Demuestra arquitectura por
capas, concurrencia con cancelación, cache en memoria con expiración y tests unitarios.

> La prueba menciona SwiftUI; al ser un perfil Android, la UI está hecha con su equivalente declarativo,
> **Jetpack Compose**.

## API pública: Open Library

Gratuita y sin API key ([docs](https://openlibrary.org/developers/api)).

| Uso | Endpoint |
| --- | --- |
| Búsqueda | `GET https://openlibrary.org/search.json?q={query}&limit=30&fields=key,title,author_name,first_publish_year,cover_i,edition_count` |
| Detalle | `GET https://openlibrary.org/works/{id}.json` |
| Portadas | `https://covers.openlibrary.org/b/id/{coverId}-{S\|M\|L}.jpg` |

- **Listado**: portada, título, autores y año de primera publicación.
- **Detalle**: portada, título, fecha de primera publicación, temas (*subjects*) y descripción (en Markdown, renderizada con enlaces y formato mediante [multiplatform-markdown-renderer](https://github.com/mikepenz/multiplatform-markdown-renderer)).

## Cómo ejecutar

Requisitos: Android Studio reciente, JDK 21 y Android SDK 37.

```bash
./gradlew installDebug                                                 # instalar en un dispositivo/emulador
./gradlew :domain:test :data:testDebugUnitTest :app:testDebugUnitTest  # tests unitarios
```

O abrir la carpeta en Android Studio y ejecutar la configuración `app`.

## Arquitectura

Tres módulos Gradle con dependencias en una sola dirección (`app → data → domain`):

```
:domain  (Kotlin/JVM puro, sin Android)
   model/       Book, BookDetail, BookError
   repository/  BookRepository (interfaz)
   usecase/     SearchBooksUseCase, GetBookDetailUseCase

:data    (Android library)
   remote/      OpenLibraryApi (Retrofit) + DTOs
   mapper/      DTO → modelo de dominio
   cache/       InMemoryCache<K, V> con TTL
   repository/  BookRepositoryImpl + mapeo de errores
   di/          Módulo Hilt (OkHttp, Retrofit, cache, binding del repositorio)

:app     (presentación)
   ui/search    SearchViewModel + SearchScreen
   ui/detail    DetailViewModel + DetailScreen
   ui/navigation  Navigation Compose con rutas tipadas (@Serializable)
```

- **Domain** no conoce Retrofit, Android ni la cache: define *qué* se puede hacer y los errores de negocio
  (`BookError`). Los repositorios devuelven `Result<T>`; los fallos se modelan, no se lanzan.
- **Data** decide *de dónde* vienen los datos y traduce cualquier fallo técnico (`IOException`, `HttpException`,
  serialización) a `BookError`.
- **Presentation** sigue un MVI ligero: cada pantalla expone un único `StateFlow<UiState>` inmutable y recibe acciones
  (`SearchAction`). Los composables sin estado (`SearchScreen`, `DetailScreen`) se pueden previsualizar.
- **DI** con Hilt. El `DetailViewModel` recibe el id del libro mediante *assisted injection*, sin depender de
  `SavedStateHandle`, lo que lo hace trivial de testear.

## Concurrencia y cancelación

Toda la lógica está en `SearchViewModel`:

```kotlin
searchRequests()                       // texto (trim + distinctUntilChanged) y reintentos
    .flatMapLatest { request ->        // ← cancela la búsqueda anterior
        flow {
            emit(Loading)              // los resultados viejos desaparecen al instante
            delay(DEBOUNCE)            // debounce, también cancelable
            emit(searchBooks(request.query).toContent())
        }
    }
    .onEach { content -> _uiState.update { it.copy(content = content) } }
    .launchIn(viewModelScope)
```

- Con `flatMapLatest`, cada nueva query **cancela la corrutina de la anterior**, tanto si estaba esperando el
  *debounce* como si la petición HTTP estaba en vuelo. Las funciones `suspend` de Retrofit cancelan la `Call` de
  OkHttp, así que la petición se aborta de verdad.
- Una corrutina cancelada no emite nada, así que **es imposible que los resultados de una búsqueda antigua
  sobrescriban los de la nueva**, aunque su respuesta llegue más tarde.
- El *debounce* va dentro del `flatMapLatest` (un `delay`) y no como operador `debounce()`. Así, el estado pasa a
  `Loading` en cuanto cambia el texto y nunca se ven resultados que no corresponden a lo escrito.
- `CancellationException` nunca se captura como error (`safeApiCall` la relanza), para respetar la concurrencia
  estructurada.
- El texto del campo (`query`) se actualiza de forma síncrona en el estado, para que el `TextField` no tenga lag.

`SearchViewModelTest` lo verifica con un repositorio falso cuyas peticiones quedan "en vuelo". Comprueba que la
búsqueda anterior se cancela y que su respuesta tardía se ignora. Si se sustituye `flatMapLatest` por
`flatMapConcat`, ese test falla.

## Cache

`InMemoryCache<K, V>` (en `:data`) es un `Map` protegido por un `Mutex`. Cada entrada guarda un `TimeMark` y caduca
a los **3 minutos**.

En `BookRepositoryImpl.searchBooks(query)`:
1. La clave se normaliza (`trim().lowercase()`): `" Dune"` y `"dune"` comparten entrada.
2. Si hay una entrada válida, se devuelve **sin llamar a red**.
3. Si no existe o ha caducado, se pide a red, se guarda y se devuelve. Las entradas caducadas se eliminan al leerlas.
4. Los errores **no se cachean**: el siguiente intento vuelve a ir a red.

El tiempo se mide con un `TimeSource` inyectable (monotónico por defecto, inmune a cambios del reloj del
dispositivo). En los tests se usa `TestTimeSource` de la stdlib de Kotlin para avanzar el tiempo de forma
determinista, sin `Thread.sleep`.

La cache es un singleton de Hilt, así que sobrevive a la navegación y a las rotaciones durante la vida del proceso.

## Tests

JUnit 5, `kotlin.test`, `kotlinx-coroutines-test` y MockK.

| Módulo | Test | Qué cubre |
| --- | --- | --- |
| domain | `SearchBooksUseCaseTest`, `GetBookDetailUseCaseTest` | Repositorio mockeado con MockK: delegación, trim, query/id vacíos sin tocar el repositorio, propagación de errores |
| data | `BookRepositoryImplTest` | Acierto de cache sin red, normalización de la clave, queries independientes, expiración y refresco, errores no cacheados, mapeo de errores HTTP, propagación de la cancelación |
| data | `InMemoryCacheTest` | Límite exacto del TTL, sobrescritura, `clear` |
| data | `BookMappersTest` | DTO → dominio, descripción como texto u objeto, normalización de saltos de línea, portadas inválidas (`-1`) |
| app | `SearchViewModelTest` | Debounce, **cancelación de la búsqueda en vuelo**, limpiar el campo, vacío, error y reintento |
| app | `DetailViewModelTest` | Carga, error y reintento |

## Posibles mejoras

- Paginación (`page` / `offset`) con Paging 3.
- Deduplicar peticiones concurrentes de la misma query (compartir un `Deferred` en vuelo).
- Cache persistente (Room/DataStore) y límite de tamaño (LRU) para la de memoria.
- Cache del detalle y resolución de los nombres de autor del *work* (`/authors/{id}.json`).
- Tests de UI con Compose (`createComposeRule`) y tests de red con `MockWebServer`.
