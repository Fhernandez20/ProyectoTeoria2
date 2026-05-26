# Database Manager Tool
**Teoría de Base de Datos II — Proyecto 1**  
**Autor:** Fernando Hernández | **Motor:** MariaDB | **Lenguaje:** Java 17 + JavaFX

---

## 📌 Descripción del Proyecto

Herramienta administrativa de base de datos que permite gestionar objetos MariaDB (tablas, vistas, procedimientos, funciones, triggers, usuarios) a través de una **interfaz gráfica interactiva**, usando **system tables nativas** del SGBD en lugar de `information_schema`.

---

## 📂 Estructura del Proyecto

```
DatabaseManager/
├── src/main/
│   ├── java/
│   │   ├── app/
│   │   │   └── MainApp.java              ← Punto de entrada JavaFX
│   │   ├── model/
│   │   │   └── ConexionGuardada.java     ← POJO de conexión
│   │   ├── service/
│   │   │   ├── ConexionManager.java      ← Gestión conexión JDBC
│   │   │   ├── ConexionStorage.java      ← Persistencia JSON
│   │   │   └── MetadataExtractor.java    ← Queries a system tables
│   │   └── controller/
│   │       ├── LoginController.java      ← Pantalla de login
│   │       └── MainController.java       ← Ventana principal + tabs
│   └── resources/
│       ├── fxml/
│       │   ├── LoginView.fxml
│       │   └── MainView.fxml
│       └── css/
│           ├── style.css
│           └── tabs.css
├── pom.xml
├── README.md
└── LIMITACIONES.md                       ← Restricciones del SGBD
```

---

## 🚀 Cómo Ejecutar

### Requisitos
- **Java 17+** (con javac)
- **Maven 3.8+**
- **MariaDB 10.0+** (corriendo localmente o en red)

### Pasos

1. **Clonar o descargar el proyecto**
```bash
cd DatabaseManager
```

2. **Compilar y ejecutar**
```bash
mvn clean javafx:run
```

3. **La aplicación abrirá**
   - Ventana de Login
   - Ingresa credenciales de MariaDB
   - Haz clic en **CONECTAR**

---

## 📊 System Tables Utilizadas (SIN information_schema)

El proyecto usa **comandos nativos de MariaDB** en lugar de `information_schema`:

### Objetos de Base de Datos

| Objeto | Comando/System Table | Método en código |
|--------|----------------------|------------------|
| **Bases de datos** | `SHOW DATABASES` | `cargarBasesDeDatos()` |
| **Tablas** | `SHOW FULL TABLES WHERE Table_type = 'BASE TABLE'` | `cargarTablas()` |
| **Vistas** | `SHOW FULL TABLES WHERE Table_type = 'VIEW'` | `cargarVistas()` |
| **Procedimientos** | `SHOW PROCEDURE STATUS WHERE Db = ?` | `cargarProcedimientos()` |
| **Funciones** | `SHOW FUNCTION STATUS WHERE Db = ?` | `cargarFunciones()` |
| **Triggers** | `SHOW TRIGGERS` | `cargarTriggers()` |
| **Usuarios** | `SELECT User, Host FROM mysql.user` | `cargarUsuarios()` |
| **Índices** | `SHOW INDEX FROM tabla` | `MetadataExtractor.listarIndices()` |

### Generación de DDL

| Objeto | Comando |
|--------|---------|
| **DDL Tabla** | `SHOW CREATE TABLE nombre` |
| **DDL Vista** | `SHOW CREATE VIEW nombre` |
| **DDL Procedimiento** | `SHOW CREATE PROCEDURE nombre` |
| **DDL Función** | `SHOW CREATE FUNCTION nombre` |
| **DDL Trigger** | `SHOW CREATE TRIGGER nombre` |

---

## 🎯 Características Principales

### 1. **Gestión de Conexiones** (10 pts)
✅ Login con cualquier usuario MariaDB válido  
✅ Múltiples conexiones guardadas en `~/.dbmanager/conexiones.json`  
✅ Botones para probar, guardar, eliminar y conectar  

### 2. **Administración de Objetos** (30 pts)
✅ **Tablas:** Listar, ver DDL, crear visualmente  
✅ **Vistas:** Listar, ver DDL, crear visualmente  
✅ **Procedimientos:** Listar, ver DDL  
✅ **Funciones:** Listar, ver DDL  
✅ **Triggers:** Listar, ver DDL  
✅ **Usuarios:** Listar con host  
✅ Árbol jerárquico en panel izquierdo  

### 3. **Operaciones sobre Objetos** (40 pts)
✅ **Tab DDL:** Mostrar `CREATE TABLE/VIEW/PROCEDURE/FUNCTION/TRIGGER`  
✅ **Editor Visual:**
   - Crear tablas con definición de columnas (soporta enters y espacios)
   - Crear vistas con consultas SELECT
   - Validación de campos vacíos
   - Validación de objetos existentes

✅ **Modificación:** Exportar DDL como SQL para editar manualmente  

### 4. **Ejecución de SQL** (15 pts)
✅ SELECT con visualización de resultados en TableView  
✅ INSERT, UPDATE, DELETE con confirmación de filas afectadas  
✅ Manejo de errores SQL descriptivos  

### 5. **Consideraciones Técnicas** (5 pts)
✅ **Sin librerías prohibidas** (Hibernate, SQLAlchemy, Dapper, etc.)  
✅ **Sin information_schema** (solo system tables nativos)  
✅ **Desktop GUI** (JavaFX, no consola)  
✅ **Documentación de limitaciones** en `LIMITACIONES.md`  

---

## 📋 Funciones Principales del Código

### `MetadataExtractor.java`
```java
// Listar objetos desde system tables
List<String> listarTablas()
List<String> listarVistas()
List<String> listarProcedimientos()
List<String> listarFunciones()
List<Map<String, String>> listarTriggers()

// Obtener DDL
String getDDLTabla(String nombre)
String getDDLVista(String nombre)
String getDDLProcedimiento(String nombre)
String getDDLFuncion(String nombre)
String getDDLTrigger(String nombre)

// Crear objetos
String crearTabla(String nombre, List<String> columnas)
String crearVista(String nombre, String selectQuery)

// Ejecutar SQL
int ejecutarDML(String sql)
List<Map<String, String>> ejecutarSelectRetornarMapa(String sql)
```

### `MainController.java`
```java
// Tabs
crearTabDDL()              // Visualizar DDL
crearTabEditorVisual()     // Crear tablas/vistas
crearTabSQLExecutor()      // Ejecutar SELECT, INSERT, UPDATE, DELETE

// Árbol
cargarArbolObjetos()       // Cargar todo desde system tables
detectarTipoObjeto()       // Identificar tipo seleccionado
```

---

## 💾 Almacenamiento de Conexiones

Las conexiones se guardan en formato JSON:

```json
[
  {
    "alias": "Prueba 2",
    "host": "localhost",
    "puerto": 3306,
    "usuario": "root",
    "contrasena": "password",
    "baseDatos": "presupuesto_personal"
  }
]
```

**Ubicación:** `~/.dbmanager/conexiones.json`  
**No se sube a GitHub:** Agregar a `.gitignore`

---

## 🎨 Interfaz Gráfica

### Tema
- **Dark Industrial / Techno-minimalista**
- Colores: Verde (#4ade80), Cyan (#22d3ee), Gris oscuro (#0f1117)
- Fuente: JetBrains Mono, Courier New

### Componentes
- **Login:** Formulario de conexión + lista de guardadas
- **Main Window:** Árbol de objetos + 3 tabs
  - **DDL:** Visualizar CREATE statements
  - **Editor Visual:** Crear tablas/vistas gráficamente
  - **Ejecutor SQL:** Correr queries y ver resultados

---

## ⚠️ Limitaciones del SGBD

Ver archivo **`LIMITACIONES.md`** para detalles completos.

**Resumen:**
- ❌ **SEQUENCE:** No soportado en MariaDB < 10.3 → Usar AUTO_INCREMENT
- ❌ **Tablespaces custom:** No soportado → Usar defaults del SGBD
- ✅ **Índices:** Listables pero no creables visualmente (no obligatorio)

---

## 🧪 Ejemplos de Uso

### Crear Tabla
1. Ir a tab **"Editor Visual"**
2. Nombre: `productos`
3. Columnas:
```
id INT AUTO_INCREMENT PRIMARY KEY, nombre VARCHAR(150) NOT NULL, precio DOUBLE NOT NULL, stock INT DEFAULT 0
```
4. Click **"Crear Tabla"**

### Crear Vista
1. Ir a tab **"Editor Visual"**
2. Nombre: `vista_productos`
3. SELECT: `SELECT id, nombre, precio FROM productos WHERE stock > 0`
4. Click **"Crear Vista"**

### Ver DDL
1. Click **"Refrescar"** en el árbol
2. Expandir **"Tablas"** → seleccionar **"productos"**
3. Ir a tab **"DDL"** → Click **"Cargar DDL"**

### Ejecutar Query
1. Ir a tab **"Ejecutor SQL"**
2. Escribir: `SELECT * FROM productos`
3. Click **"Ejecutar"**
4. Ver resultados en tabla

---

## 📝 Notas Técnicas

- **Validación:** Verifica tablas/vistas existentes antes de crear
- **Limpieza de entrada:** Elimina enters y espacios extras en columnas
- **Threads:** Conexiones DB en threads separados para no bloquear UI
- **Error handling:** Mensajes descriptivos en status bar

---

## 📚 Dependencias

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>21.0.2</version>
</dependency>
<dependency>
    <groupId>org.mariadb.jdbc</groupId>
    <artifactId>mariadb-java-client</artifactId>
    <version>3.3.3</version>
</dependency>
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>
```

---

## ✅ Rúbrica de Evaluación (100 pts)

| Criterio | Puntaje | Status |
|----------|---------|--------|
| Gestión de conexiones | 10 | ✅ |
| Administración de objetos | 30 | ✅ |
| Operaciones sobre objetos | 40 | ✅ |
| Ejecución de SQL | 15 | ✅ |
| Consideraciones técnicas | 5 | ✅ |
| **TOTAL** | **100** | **✅** |

---

## 🔒 Restricciones Cumplidas

✅ **Sin information_schema** - Usa SHOW commands nativos  
✅ **Sin ORM/Frameworks prohibidos** - JDBC directo  
✅ **Desktop/Web** - JavaFX Desktop  
✅ **System tables explícitas** - Documentadas en este README  
✅ **Documentación de limitaciones** - Ver LIMITACIONES.md  

---

## 👤 Autor

**Fernando Hernández**  
Proyecto 1 - Teoría de Base de Datos II  
2026-05-24

---

**Última actualización:** 2026-05-24
