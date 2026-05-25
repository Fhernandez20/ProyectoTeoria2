# Database Manager Tool
**Teoría de Base de Datos II — Proyecto 1**
**Autor:** Fernando Hernández | Motor: **MariaDB**

---

## Estructura del proyecto

```
DatabaseManager/
├── pom.xml
└── src/main/
    ├── java/
    │   ├── app/
    │   │   └── MainApp.java                ← Punto de entrada JavaFX
    │   ├── model/
    │   │   └── ConexionGuardada.java        ← POJO de una conexión guardada
    │   ├── service/
    │   │   ├── ConexionManager.java         ← Gestión de conexión JDBC activa
    │   │   ├── ConexionStorage.java         ← Persistencia JSON local (Gson)
    │   │   └── MetadataExtractor.java       ← Consultas a system tables de MariaDB
    │   └── controller/
    │       ├── LoginController.java         ← Pantalla de login / gestión de conexiones
    │       └── MainController.java          ← Ventana principal: árbol, tabs, SQL
    └── resources/
        ├── fxml/
        │   ├── LoginView.fxml
        │   └── MainView.fxml
        └── css/
            ├── style.css
            └── tabs.css
```

---

## Cómo ejecutar

### Requisitos
- Java 17 o superior
- Maven 3.8 o superior
- MariaDB corriendo (local o remoto)

### Comando
```bash
mvn clean javafx:run
```

---

## System tables de MariaDB usadas

El proyecto **NO usa `information_schema`**. Todas las consultas de metadata son directas:

| Objeto               | Comando / System table                                      |
|----------------------|-------------------------------------------------------------|
| Tablas               | `SHOW FULL TABLES WHERE Table_type = 'BASE TABLE'`          |
| Vistas               | `SHOW FULL TABLES WHERE Table_type = 'VIEW'`                |
| Secuencias           | `SHOW FULL TABLES WHERE Table_type = 'SEQUENCE'`            |
| DDL de tabla         | `SHOW CREATE TABLE <nombre>`                                |
| DDL de vista         | `SHOW CREATE VIEW <nombre>`                                 |
| Procedimientos       | `SHOW PROCEDURE STATUS WHERE Db = ?`                        |
| DDL de procedimiento | `SHOW CREATE PROCEDURE <nombre>`                            |
| Funciones            | `SHOW FUNCTION STATUS WHERE Db = ?`                         |
| DDL de función       | `SHOW CREATE FUNCTION <nombre>`                             |
| Triggers             | `SHOW TRIGGERS`                                             |
| DDL de trigger       | `SHOW CREATE TRIGGER <nombre>`                              |
| Índices              | `SHOW INDEX FROM <tabla>`                                   |
| DDL de índice        | Reconstruido desde `SHOW INDEX FROM` (ver justificación)    |
| Secuencias DDL       | `SHOW CREATE SEQUENCE <nombre>`                             |
| Usuarios             | `SELECT User, Host FROM mysql.user`                         |
| DDL de usuario       | `SHOW CREATE USER <user>@<host>`                            |
| Grants               | `SHOW GRANTS FOR <user>@<host>`                             |
| Storage info         | `SHOW TABLE STATUS` (equivalente a tablespaces en MariaDB)  |
| Columnas             | `DESCRIBE <tabla>`                                          |
| Info conexión        | `DatabaseMetaData` de JDBC (no consulta externa)            |

---

## Documentación de limitaciones por objeto

### ✅ Objetos implementados

| Objeto               | Estado      | Notas                                          |
|----------------------|-------------|------------------------------------------------|
| Tablas               | ✅ Completo  | Listado, DDL, creación visual, columnas        |
| Vistas               | ✅ Completo  | Listado, DDL, creación visual                  |
| Procedimientos       | ✅ Completo  | Listado y DDL                                  |
| Funciones            | ✅ Completo  | Listado y DDL                                  |
| Triggers             | ✅ Completo  | Listado y DDL                                  |
| Índices              | ✅ Completo  | Listado agrupado por tabla, DDL reconstruido    |
| Secuencias           | ✅ Parcial   | Requiere MariaDB 10.3+ (ver abajo)             |
| Usuarios             | ✅ Completo  | Listado desde `mysql.user`, DDL y GRANTS       |
| Tablespaces          | ⚠️ Limitado | Ver justificación abajo                         |
| Paquetes             | ❌ N/A       | No existe en MariaDB (ver justificación)        |

---

### ❌ Paquetes (Packages) — NO APLICA EN MariaDB

Los paquetes (`PACKAGE` / `PACKAGE BODY`) son una construcción exclusiva de
**Oracle PL/SQL** y **IBM DB2**. Permiten agrupar procedimientos y funciones
relacionados en una unidad lógica reutilizable.

**MariaDB no implementa paquetes.** No existe una system table ni comando
`SHOW PACKAGES` en ninguna versión de MariaDB. Este objeto no aplica para este
SGBD y no es posible justificarlo técnicamente de otra forma.

> Referencia: [MariaDB Knowledge Base — Stored Routines](https://mariadb.com/kb/en/stored-routines/)

---

### ⚠️ Tablespaces — SOPORTE LIMITADO EN MariaDB

#### Situación técnica

MariaDB/InnoDB maneja los tablespaces de forma interna. A diferencia de Oracle,
**no existe una system table nativa accesible sin `information_schema`** que liste
los tablespaces como objetos de primera clase.

Las únicas alternativas disponibles son:

| Opción | Disponibilidad | Motivo de no uso |
|--------|---------------|-----------------|
| `information_schema.INNODB_TABLESPACES` | ✅ | **PROHIBIDO** en este proyecto |
| `information_schema.TABLES` | ✅ | **PROHIBIDO** en este proyecto |
| `SHOW TABLE STATUS` | ✅ **Usado** | Expone engine, row_format y data_length por tabla |

#### Solución adoptada

Se usa `SHOW TABLE STATUS`, que es un comando nativo de MariaDB que devuelve
información del storage engine por tabla. En MariaDB, el engine InnoDB gestiona
el tablespace, y esta información es la más cercana disponible sin
`information_schema`.

El árbol muestra los engines distintos presentes en la base de datos. La pestaña
**"Info Objetos"** muestra la tabla completa de `SHOW TABLE STATUS` con columnas
`Engine`, `Rows`, `Data_length` y `Row_format`.

---

### ⚠️ Secuencias — REQUIERE MariaDB 10.3+

MariaDB introdujo soporte nativo de secuencias en la versión **10.3.0** (2018).

#### Cómo se detectan sin `information_schema`

A partir de MariaDB 10.3, las secuencias aparecen en el catálogo con un tipo
especial accesible mediante:

```sql
SHOW FULL TABLES WHERE Table_type = 'SEQUENCE';
```

Este comando usa el mismo mecanismo que `SHOW FULL TABLES` para tablas y vistas,
pero filtra por el tipo `SEQUENCE` registrado directamente en el catálogo del
servidor. **No pasa por `information_schema`.**

El DDL se obtiene con:
```sql
SHOW CREATE SEQUENCE <nombre>;
```

#### Comportamiento en versiones anteriores

En MariaDB < 10.3, la consulta no retorna resultados (la cláusula
`WHERE Table_type = 'SEQUENCE'` simplemente no matchea nada). El árbol mostrará
el nodo "Secuencias" vacío con el mensaje `(ninguna — requiere MariaDB 10.3+)`.

---

### ⚠️ Índices — DDL reconstruido

MariaDB **no implementa `SHOW CREATE INDEX`** (a diferencia de MySQL 8.0+ que
tampoco lo tiene). El DDL de un índice se reconstruye desde los datos de
`SHOW INDEX FROM <tabla>`, que devuelve:

- `Key_name` — nombre del índice
- `Column_name` — columna(s) indexada(s)
- `Non_unique` — 0 = UNIQUE, 1 = no único
- `Index_type` — BTREE, HASH, FULLTEXT, etc.

El resultado es un `CREATE INDEX` funcionalmente equivalente al original.
Para `PRIMARY KEY`, se redirige al `SHOW CREATE TABLE` que lo incluye.

---

## Conexiones guardadas

Se almacenan en: `~/.dbmanager/conexiones.json`

> **Nota de seguridad:** Las contraseñas se guardan en texto plano por simplicidad
> académica. En un entorno de producción se usaría cifrado (AES/Keystore).

Agregar a `.gitignore`:
```
.dbmanager/
target/
```

---

## Fases del proyecto — Estado final

- [x] Fase 1: Gestión de conexiones y login (guardar, eliminar, probar, conectar)
- [x] Fase 2: Listado completo de objetos + DDL desde system tables
- [x] Fase 3: Creación visual de tablas, vistas e índices
- [x] Fase 4: Ejecución de sentencias SQL con resultados en tabla
- [x] Fase 5: Documentación de limitaciones del SGBD
