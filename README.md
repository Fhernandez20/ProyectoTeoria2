# Database Manager Tool
**Teoría de Base de Datos II — Proyecto 1**
**Autor:** Fernando Hernández | Motor: MariaDB

---

## Estructura del proyecto

```
DatabaseManager/
├── pom.xml
└── src/main/
    ├── java/
    │   ├── app/
    │   │   └── MainApp.java              ← Punto de entrada JavaFX
    │   ├── model/
    │   │   └── ConexionGuardada.java     ← POJO de una conexión
    │   ├── service/
    │   │   ├── ConexionManager.java      ← Conexión JDBC activa
    │   │   └── ConexionStorage.java      ← Persistencia JSON local
    │   └── controller/
    │       ├── LoginController.java      ← Pantalla de login
    │       └── MainController.java       ← Ventana principal + árbol de objetos
    └── resources/
        ├── fxml/
        │   ├── LoginView.fxml
        │   └── MainView.fxml
        └── css/
            └── style.css
```

---

## Cómo ejecutar

### Requisitos
- Java 17+
- Maven 3.8+
- MariaDB corriendo localmente (o remoto)

### Comando
```bash
mvn clean javafx:run
```

---

## System tables de MariaDB usadas

El proyecto NO usa `information_schema`. En cambio usa:

| Objetivo                | Query / System table            |
|-------------------------|---------------------------------|
| Listar bases de datos   | `SHOW DATABASES`                |
| Listar tablas           | `SHOW FULL TABLES WHERE Table_type = 'BASE TABLE'` |
| Listar vistas           | `SHOW FULL TABLES WHERE Table_type = 'VIEW'`       |
| Listar procedimientos   | `SHOW PROCEDURE STATUS WHERE Db = ?`               |
| Listar funciones        | `SHOW FUNCTION STATUS WHERE Db = ?`                |
| Listar triggers         | `SHOW TRIGGERS`                                    |
| Listar usuarios         | `SELECT User, Host FROM mysql.user`                |

---

## Fases del proyecto

- [x] Fase 1: Gestión de conexiones y login
- [ ] Fase 2: Listado completo de objetos + DDL desde system tables
- [ ] Fase 3: Creación visual de tablas y vistas
- [ ] Fase 4: Ejecución de sentencias SQL con resultados

---

## Conexiones guardadas

Se almacenan en: `~/.dbmanager/conexiones.json`
(Archivo local, no se sube al repositorio — agregar a `.gitignore`)
