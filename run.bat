@echo off
rem Запуск: sapjco3.dll должна лежать в lib\ и соответствовать разрядности JVM
java -Djava.library.path=lib -cp target\passwd_changer-2.2.jar;lib\sapjco3.jar passwd_changer.Main
