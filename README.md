# 7xParkour

Plugin de parkours para Paper 1.20 - 26.x (Java 17 / 21 / 25).

## Compilar

```bash
unzip 7xParkour.zip
cd 7xParkour
mvn clean package
```

El jar queda en `target/7xParkour-1.0.0.jar`. Requiere JDK 17+ y Maven.

## Instalar

Copiar el jar a `plugins/` y reiniciar el servidor.

## Comandos

`/pk help` muestra todos los comandos.

| Comando | Permiso |
|---|---|
| `/pk create <nombre>` | `xparkour.admin` |
| `/pk setstart <parkour>` | `xparkour.admin` |
| `/pk addcp <parkour>` | `xparkour.admin` |
| `/pk setend <parkour>` | `xparkour.admin` |
| `/pk join <parkour>` | `xparkour.play` |
| `/pk top <parkour>` | `xparkour.play` |

## Licencia

MIT
