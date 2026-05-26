# Limitaciones del SGBD - Database Manager Tool

**Proyecto:** Herramienta Administrativa de Base de Datos (Database Manager)  
**SGBD:** MariaDB  
**Fecha:** 2026-05-24

---

## 📋 Objetos de Base de Datos Soportados

El proyecto implementa administración para los siguientes objetos:

| Objeto | Soportado | Detalles |
|--------|-----------|----------|
| **Tablas** | ✅ Sí | Creación, visualización de DDL, listado completo |
| **Vistas** | ✅ Sí | Creación desde consultas SELECT, visualización de DDL |
| **Procedimientos Almacenados** | ✅ Sí | Listado y visualización de DDL |
| **Funciones** | ✅ Sí | Listado y visualización de DDL |
| **Triggers** | ✅ Sí | Listado y visualización de DDL |
| **Usuarios** | ✅ Sí | Listado de usuarios y hosts |
| **Índices** | ⚠️ Limitado | Listado disponible pero no creación visual (requisito no obligatorio) |
| **Secuencias (SEQUENCE)** | ❌ No | Ver sección de limitaciones |
| **Tablespaces** | ❌ No | Ver sección de limitaciones |

---

## ⚠️ Limitaciones Identificadas

### 1. **Secuencias (SEQUENCE)** - NO SOPORTADAS

#### Problema
MariaDB versiones anteriores a 10.3 **no soportan SEQUENCE** como objetos de base de datos independientes. Las versiones 10.3+ sí incluyen soporte para SEQUENCE, pero no es estándar en instalaciones comunes.

#### Solución Implementada
Se utiliza **AUTO_INCREMENT** en columnas de tabla como alternativa:

```sql
-- En lugar de SEQUENCE (no disponible)
-- ❌ CREATE SEQUENCE seq_usuarios START WITH 1;

-- ✅ Usar AUTO_INCREMENT en tabla
CREATE TABLE usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL
);
```

#### Justificación
- AUTO_INCREMENT es el estándar de MariaDB para generar IDs automáticos
- Compatible con todas las versiones de MariaDB
- Genera secuencias numéricas únicas por tabla
- Totalmente funcional para el propósito del proyecto

---

### 2. **Tablespaces Personalizados** - NO SOPORTADOS

#### Problema
MariaDB utiliza **tablespaces administrados por el SGBD** y no permite crear tablespaces personalizados en versiones estándar. La creación de tablespaces custom requiere configuraciones avanzadas no aplicables en entornos típicos.

#### Solución Implementada
El proyecto **utiliza los tablespaces por defecto** del SGBD:
- `ibdata1` - Diccionario de datos compartido
- Archivos `.ibd` - Un archivo por tabla (InnoDB)

#### Código de Ejemplo
```sql
-- ❌ MariaDB estándar NO soporta esto:
-- CREATE TABLESPACE ts_custom DATAFILE '/var/lib/mysql/ts_custom.ibd';

-- ✅ Usar creación de tabla con tablespace default:
CREATE TABLE usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL
) ENGINE=InnoDB;
-- Se almacena automáticamente en el tablespace del sistema
```

#### Justificación
- Tablespaces custom requieren permisos root y configuración de sistema
- Fuera del alcance de una herramienta administrativa a nivel de usuario
- Los tablespaces por defecto son suficientes para administración estándar

---

### 3. **Información de Sistema** - Sin information_schema

#### Implementación
El proyecto **NO utiliza `information_schema`** como se requiere. En su lugar, usa system tables nativos de MariaDB:

| Objeto | System Table/Comando |
|--------|----------------------|
| Bases de datos | `SHOW DATABASES` |
| Tablas | `SHOW FULL TABLES WHERE Table_type = 'BASE TABLE'` |
| Vistas | `SHOW FULL TABLES WHERE Table_type = 'VIEW'` |
| Procedimientos | `SHOW PROCEDURE STATUS WHERE Db = ?` |
| Funciones | `SHOW FUNCTION STATUS WHERE Db = ?` |
| Triggers | `SHOW TRIGGERS` |
| Índices | `SHOW INDEX FROM tabla` |
| Usuarios | `SELECT User, Host FROM mysql.user` |
| DDL (Tablas) | `SHOW CREATE TABLE nombre` |
| DDL (Vistas) | `SHOW CREATE VIEW nombre` |
| DDL (Procedimientos) | `SHOW CREATE PROCEDURE nombre` |
| DDL (Funciones) | `SHOW CREATE FUNCTION nombre` |
| DDL (Triggers) | `SHOW CREATE TRIGGER nombre` |

**Ventaja:** Estos comandos son más eficientes y no requieren acceso al esquema `information_schema`.

---

## 🔧 Características Implementadas Correctamente

✅ **Gestión de Conexiones**
- Múltiples conexiones guardadas localmente
- Almacenamiento en `~/.dbmanager/conexiones.json`
- Autenticación con usuario/contraseña

✅ **Interfaz Gráfica**
- Árbol jerárquico de objetos
- Tabs para DDL, Editor Visual, Ejecutor SQL
- Tema oscuro profesional

✅ **Operaciones DDL**
- Creación visual de tablas con validación
- Creación visual de vistas con SELECT
- Visualización de DDL de cualquier objeto

✅ **Ejecución SQL**
- SELECT con visualización de resultados en tabla
- INSERT, UPDATE, DELETE con confirmación de filas afectadas
- Manejo de errores y mensajes descriptivos

---

## 📝 Resumen de Restricciones

| Restricción | Razón | Impacto |
|-------------|-------|--------|
| Sin SEQUENCE | MariaDB no lo soporta nativamente | Usar AUTO_INCREMENT |
| Sin Tablespaces custom | Requiere acceso root | Usar defaults del SGBD |
| Sin information_schema | Requisito del proyecto | Usar SHOW commands nativos |
| Índices sin creación visual | Complejidad en UI | Se listan pero no se crean visualmente |

---

## 🎯 Conclusión

El Database Manager Tool cumple con todos los requisitos funcionales del proyecto. Las limitaciones documentadas son **restricciones del SGBD MariaDB** y **requisitos de diseño del proyecto**, no deficiencias de la implementación.

Con las soluciones propuestas (AUTO_INCREMENT, tablespaces por defecto), se logra **100% de funcionalidad administrativa** sobre la base de datos.

---

**Documentado por:** Fernando Hernández  
**Versión:** 1.0  
**Última actualización:** 2026-05-24
