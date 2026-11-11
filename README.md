# Book Search: Open Library demo (Android)

App Android que busca libros en [Open Library](https://openlibrary.org/developers/api) y muestra su detalle.
Hecha con Kotlin, Jetpack Compose, Coroutines/Flow, Hilt y Retrofit.

- **Búsqueda**: portada, título, autores y año de primera publicación.
- **Detalle**: portada, fecha de publicación, temas y descripción (Markdown con enlaces).
- **Acerca de**: botón de información que muestra este README.

## Arquitectura

Tres módulos con dependencias en una sola dirección (`app → data → domain`):

- **domain**: Kotlin puro. Modelos, `BookRepository`, casos de uso y errores de negocio (`BookError`).
- **data**: Retrofit, DTOs, cache en memoria y mapeo de cualquier fallo técnico a `BookError`.
- **app**: Compose con MVI ligero (un `StateFlow<UiState>` inmutable por pantalla) y Navigation con rutas tipadas.

## Concurrencia y cancelación

Toda la lógica está en `SearchViewModel`:

```kotlin
searchRequests()                       // texto (trim + distinctUntilChanged) y reintentos
    .flatMapLatest { request ->        // cancela la búsqueda anterior
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

Unitarios con JUnit 5, `kotlinx-coroutines-test` y MockK en los tres módulos: casos de uso, repositorio y cache
(con tiempo virtual), mappers y ViewModels, incluida la cancelación de la búsqueda en vuelo.
