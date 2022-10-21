@echo off
rem Запуск: sapjco3.dll должна лежать в lib\ и соответствовать разрядности JVM
java -Djava.library.path=lib -cp target\kennw-andern-2.2.jar;lib\sapjco3.jar kennw.andern.Main
