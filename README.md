# Proyecto-Requerimientos-UPIICSA
Sistema de Gestión de Préstamo de Equipo Tecnológico para UPIICSA

Este proyecto consiste en desarrollar un sistema y aplicación web para administrar el inventario de equipos tecnológicos de UPIICSA, permitiendo el control de usuarios, solicitudes de préstamo y disponibilidad de equipos en tiempo real.

En esta guía se explica cómo preparar la computadora, descargar el proyecto y ejecutar la página web.

## 1. Programas que necesitas instalar

Antes de comenzar, instala los siguientes programas. Si ya tienes alguno, puedes pasar al siguiente.

### 1.1 Visual Studio Code

Es el programa donde abriremos y modificaremos los archivos del proyecto.

Descarga: https://code.visualstudio.com/

Instálalo siguiendo las indicaciones del instalador.

### 1.2 Java JDK

Java es necesario para ejecutar el sistema.

Puedes descargar un JDK compatible (la version 21 de preferencia) desde: https://www.oracle.com/latam/java/technologies/downloads/#jdk21-windows 

Selecciona una versión compatible con el proyecto y sigue las instrucciones de instalación.

**Cómo comprobar que se instaló correctamente:**

1. Presiona las teclas `Windows + R`.
2. Escribe `powershell` y presiona Enter. Se abrirá una ventana donde puedes escribir comandos.
3. Escribe el siguiente comando y presiona Enter:

   `java -version`

4. Después escribe:

   `javac -version`

Si ambos comandos muestran un número de versión, Java está disponible. Si Windows indica que no reconoce el comando, puede ser necesario revisar la instalación o configurar las variables de entorno.

### 1.3 Git

Git nos permite descargar el proyecto y compartir los cambios con los demás integrantes.

Descarga: https://git-scm.com/downloads

Instálalo con las opciones predeterminadas.

Para comprobar que funciona:

1. Abre PowerShell como se explicó en el apartado anterior.
2. Escribe `git --version` y presiona Enter.
3. Si aparece un número de versión, está listo.

### 1.4 MySQL Server y MySQL Workbench

Necesitamos ambos programas para trabajar con la base de datos.

- **MySQL Server:** guarda la información del sistema.
- **MySQL Workbench:** permite administrar esa información mediante ventanas y botones, sin tener que hacer todo desde la consola.

Descarga: https://dev.mysql.com/downloads/installer/

Durante la instalación, instala MySQL Server y MySQL Workbench. Configura una contraseña para el usuario administrador de MySQL y guárdala en un lugar seguro.

## 2. Descargar el proyecto

Una vez instalados los programas, vamos a descargar el código que está en GitHub.

### Paso 1. Crear una carpeta para el proyecto

1. Abre el Explorador de archivos de Windows.
2. Entra en el disco `C:`.
3. Crea una carpeta llamada `Proyectos`.

La ubicación quedará así:

`C:\Proyectos`

### Paso 2. Abrir PowerShell en esa carpeta

1. Abre la carpeta `C:\Proyectos` en el Explorador de archivos.
2. Haz clic en la barra donde aparece la dirección de la carpeta.
3. Escribe `powershell` y presiona Enter.

Se abrirá PowerShell ubicado en esa carpeta.

### Paso 3. Descargar el repositorio

Copia este comando, pégalo en PowerShell y presiona Enter:

`git clone https://github.com/graciaserika2-jpg/Proyecto-Requerimientos-.git`

Espera a que termine. Se creará una carpeta con el proyecto.

Después entra en ella escribiendo:

`cd Proyecto-Requerimientos-`

Presiona Enter.

### Paso 4. Abrir el proyecto

1. Abre Visual Studio Code.
2. Selecciona **File > Open Folder** (Archivo > Abrir carpeta).
3. Busca `C:\Proyectos\Proyecto-Requerimientos-`.
4. Selecciona la carpeta y presiona **Select Folder** (Seleccionar carpeta).

En el panel izquierdo de VS Code deberían aparecer los archivos y carpetas del proyecto. Entre ellos debe estar `pom.xml`.

## 3. Preparar la base de datos

Cada integrante tendrá una base de datos en su propia computadora.

### Paso 1. Abrir MySQL Workbench

Abre MySQL Workbench y entra en la conexión local que configuraste durante la instalación.

Si te solicita la contraseña del usuario administrador, introduce la que configuraste al instalar MySQL.

### Paso 2. Crear la base de datos

1. Abre una pestaña SQL nueva.
2. Copia este comando:

   `CREATE DATABASE IF NOT EXISTS prestamo_equipos;`

3. Pégalo en la pestaña SQL.
4. Presiona el botón del rayo para ejecutar el comando.

La base de datos `prestamo_equipos` quedará creada.

### Paso 3. Configurar el usuario de la aplicación

El proyecto utiliza un usuario llamado `prestamo_app` para conectarse a MySQL.

En una pestaña SQL, conectado como administrador, ejecuta:

```sql
CREATE USER 'prestamo_app'@'localhost'
IDENTIFIED BY 'TU_CONTRASEÑA';

GRANT ALL PRIVILEGES ON prestamo_equipos.*
TO 'prestamo_app'@'localhost';
```

Cambia `TU_CONTRASEÑA` por una contraseña propia antes de ejecutar el código.

Si el usuario ya existe, no vuelvas a ejecutar `CREATE USER`. En ese caso, configura su contraseña con `ALTER USER` y aplica los permisos con el comando `GRANT`.

No compartas tu contraseña con el equipo ni la guardes en archivos que vayas a subir a GitHub.

## 4. Configurar la conexión con MySQL

Ahora tenemos que indicarle al proyecto cuál es la contraseña de la base de datos.

### Paso 1. Abrir PowerShell desde VS Code

1. En VS Code, abre el menú **Terminal**.
2. Selecciona **New Terminal** (Nueva terminal).
3. En la parte inferior aparecerá una terminal.

Comprueba que estás dentro de la carpeta del proyecto, donde se encuentra el archivo `pom.xml`.

### Paso 2. Introducir la contraseña

En esa terminal, escribe el siguiente comando, sustituyendo el texto de ejemplo por la contraseña que configuraste para `prestamo_app`:

`$env:DB_PASSWORD = "TU_CONTRASEÑA"`

Presiona Enter.

Este paso configura la contraseña para esa terminal. Si cierras la terminal y abres otra, tendrás que introducirla nuevamente.

**Importante:** no escribas tu contraseña en el README ni la publiques en GitHub.

## 5. Ejecutar la página web

Una vez que la base de datos y la contraseña estén configuradas, podemos iniciar el sistema.

### Paso 1. Iniciar el proyecto

En la terminal de VS Code, escribe:

`.\mvnw.cmd spring-boot:run`

Presiona Enter y espera a que termine de iniciar.

La primera vez puede tardar algunos minutos porque se descargarán las dependencias necesarias.

Cuando aparezca un mensaje que indique que la aplicación inició correctamente, estará lista para utilizarse.

Para terminar la ejecución de spring boot presionar Ctrl + C. Si no termina por copleto darle Enter y marcar S para terminar el trabajo por lotes

### Paso 2. Abrir la página de inventario

Abre Google Chrome u otro navegador y escribe esta dirección:

http://localhost:8080/inventario

Debería aparecer la página de inventario del sistema.

Mientras utilices la página, no cierres la terminal donde está ejecutándose el proyecto.

## 6. ¿Cómo abrir el proyecto otro día?

Si ya instalaste todo y descargaste el proyecto, no tienes que repetir la instalación.

1. Abre MySQL Server y asegúrate de que esté funcionando.
2. Abre el proyecto desde Visual Studio Code.
3. Abre **Terminal > New Terminal**.
4. Introduce nuevamente la variable de entorno con tu contraseña:

   `$env:DB_PASSWORD = "TU_CONTRASEÑA"`

5. Ejecuta:

   `.\mvnw.cmd spring-boot:run`

6. Abre el navegador y visita:

   http://localhost:8080/inventario

## 7. ¿Cómo obtener las actualizaciones de los compañeros?

Cuando alguien agregue una página o haga cambios y los suba a GitHub, podrás descargarlos sin volver a instalar todo.

Antes de actualizar, guarda tu trabajo y asegúrate de que no tienes modificaciones locales pendientes.

En la terminal de VS Code, dentro de la carpeta del proyecto, ejecuta:

`git pull`

Cuando termine, podrás iniciar nuevamente el sistema y probar los cambios.

Si tienes cambios propios sin guardar en Git, consulta con el equipo antes de actualizar para evitar perder trabajo.

## 8. Errores comunes

**No se reconoce `java`:** revisa que Java esté instalado y configurado correctamente.

**No se reconoce `git`:** revisa que Git esté instalado y vuelve a abrir PowerShell.

**Error al conectar con MySQL:** comprueba que MySQL Server esté iniciado, que la base de datos exista y que la contraseña sea correcta.

**La página no abre:** revisa que Spring Boot haya iniciado correctamente y que la terminal siga abierta.

**La página muestra un error 404:** comprueba que la ruta esté implementada en el proyecto.

## 9. Estado actual

Actualmente estamos desarrollando el módulo de inventario. Los demás módulos se irán agregando al mismo proyecto conforme avance el trabajo.

Cada integrante puede descargar el repositorio y ejecutar el sistema en su computadora. La base de datos local de cada persona es independiente, por lo que los registros de prueba no se comparten automáticamente.

Si encuentras un error que no puedas resolver, comparte el mensaje que aparece en la terminal con el equipo para revisarlo.
