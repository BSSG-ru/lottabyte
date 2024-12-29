## Lottabyte  — открытый проект BSSG по созданию набора сервисов для управления структурными элементами данных

- Сбор и мониторинг физических метаданных (Active metadata)
- Управление глоссариями и реестрами показателей (Business metadata)
- Сбор статистики чтения/записи данных (Technical metadata)
- Управление концептуальной и логической моделями данных
- Учет дата-активов и определение совокупной стоимости владения данными
### Функциональная архитектура Lottabyte

 ![Функциональная архитектура Lottabyte](https://storage.yandexcloud.net/lottabyte-doc/lb_func_arch.png)

### Функции в Lottabyte

- Инструменты учета дата-активов и дата-продуктов
- Инструменты управления глоссарием бизнес терминов и сущностей
- Построение цепочки создания ценности на основе данных (Data Lineage++)
- Инструменты управления КМД, ЛМД, ФМД
- API для работы с workflow
- Ролевая модель и сервисы безопасности с поддержкой LDAP
- Инструменты управления качеством данных на основе правил на python
- Инструменты API Governance
- Поддержка стандарта OpenLineage

###  Архитектура Lottabyte

 ![Архитектура Lottabyte](https://storage.yandexcloud.net/lottabyte-doc/lb_arch.png)

 Основное архитектурное решение базируется на том, что все компоненты Lottabyte дислоцированы в Kubernates. Опционально, компоненты могут быть развернуты без оркестратора контейнеров на физических или виртуальных сервера. Программное обеспечение Lottabyte разработано на базе концепции API-first, для реализации бизнес-логики (API) применяется Java (spring), пользовательский интерфейс разработан с использованием фреймворка React на TS. Подсистема выполнения правил качества реализовано на python.


1.	Usermgmt – компонент разработан на java (spring), отвечает за все функции API безопасности в Lottabyte.
2.	Backend – компонент разработан на java (spring), отвечает за все функции API в Lottabyte для пользовательского интерфейса и других систем интегрируемых с Lottabyte.
3.	Workflow – компонент базируется на инструменте flowable, сделан для управления и выполнения процессов согласования изменений в Lottabyte.
4.	Scheduler – компонент разработан на java (spring), отвечает за выполнения периодических операций в Lottabyte.
5.	База данных – для хранения данных применяется БД Postgresql.
6.	ElasticSearch – компонент для обеспечения задач поиска по данным Lottabyte.
7.	OlenLinage  – компонент для логирования интеграционных процессов и проверок качества данных.
8.	DQ Tool  – компонент выполнения правил качества.
9.	DQ Reports  – компонент для отображения отчетов/дашбордов проверки качества.
10.	Nginx  – Маршрутизатор запросов


  ### Инструкция по установке
  Для утановки Lottabyte необходимо установить и настроить  проект LottabyteUI, который содержит front-end (проект LottabyteUI).

  - Установить PostgreSQL версии 9 или старше;
  - Инициализировать базу данных (папка ddl проекта Lottabyte): 
1. pg_da_ddl.sql - инициализация схемы da
2. pg_usermgmt_ddl.sql - инициализация схемы usermgmt
3. pg_da_pg_versions.sql - инициализация скриптов для версионности
4. pg_da_999_ddl.sql -  инициализация тенанта 999
4. pg_da_elastic_versions.sql - инициализация elastic
  - Установить или воспользоваться сторонним сервисом S3 для хранения данных. Далее будут необходимы следующие данные:
    service-endpoint;
    signing-region;
    public-key;
    secret-key;
    bucket;
    external_bucket;
  - Установить java version 11;
  - Установить nodejs версии 16.18*;
  - Установить npm версии 8.19*;
  - Установить elastic версии 8.9.1
    Поправить конфигурационный файл
```ini
xpack.security.http.ssl:
enabled: false
keystore.path: certs/http.p12
xpack.security.transport.ssl:
enabled: false
verification_mode: certificate
keystore.path: certs/transport.p12
truststore.path: certs/transport.p12
```
  - Запустить сервис elasticsearch `systemctl restart elasticsearch.service`;
  - Выпустить ssl сертификат;
  - Установить Nginx (конфигурация по умолчанию находится в проекте Lottabyte в папке nginx)
  
    ###### Инструкция по установке coreapi
- Откомпилировать проект lottabyte, выполнив команду `mvn package  -f "lottabyte/pom.xml`
- Cкопировать в рабочую директорию файлы: 
1. coreapi_application.properties
2. coreapi-1.0-SNAPSHOT.ja
3. Отредактировать файл coreapi_application.properties:
```ini
spring.datasource.url=jdbc:postgresql://host:port/DBname хост, порт и название БД 
spring.datasource.host=Адрес хоста БД
spring.datasource.port=порт БД
spring.datasource.db=Название БД
spring.datasource.username=имя пользователя БД
spring.datasource.password=пароль пользователя БД
elasticsearch.password=Пароль пользователя elastic
storage_path= путь до папки “drivers”
service-endpoint=адрес сервиса S3
signing-region=регион
public-key=публичный ключ
secret-key=приватный ключ
bucket=название бакета
external_bucket=путь до бакета
flowable.idm.app.admin.user-id=admin
flowable.idm.app.admin.password=pass
flowable.idm.app.admin.first-name=admin
flowable.idm.app.admin.last-name=admin
flowable.common.app.idm-admin.user=admin
flowable.common.app.idm-admin.password=pass
flowable.admin.app.security.encryption.credentials-i-v-spec=pass
flowable.admin.app.security.encryption.credentials-secret-spec=pass
```
4.Сгенерировать keystore.jks. 
Пример:
```ini 
keytool -genkey -keyalg RSA -alias my_key_alias -keystore keystore.jks -storepass my_secret_password -validity 3650 -keysize 2048
```
Сгенерированный файл нужно положить в папки coreapi/usermanagement/scheduler  
``` /target/classes/keys/ ```

###### Инструкция по установке Usermanagement 

Скопировать в рабочую директорию файлы: 
- usermanagement_application.properties
- usermanagement-1.0-SNAPSHOT.jar
- Отредактировать файл usermanagement_application.properties заполнив следующие поля:
```ini
spring.datasource.url=jdbc:postgresql://host:port/ DBname хост, порт и название БД
spring.datasource.username= имя пользователя БД
spring.datasource.password= пароль пользователя БД
```

###### Инструкция по установке Scheduler

Скопировать в рабочую директорию файлы: 
- scheduler_application.properties
- scheduler-1.0-SNAPSHOT.jar
Отредактировать файл scheduler_application.properties заполнив следующие поля:
```ini
spring.datasource.url=jdbc:postgresql://host:port/ DBname хост, порт и название БД
spring.datasource.username= имя пользователя БД
spring.datasource.password= пароль пользователя БД
lottabyte.api.url= адрес сайта Lottabyte
```
###### Инструкция по установке LottabyteUI

- Установить зависимости - `npm install`.

- Выполнить команды, где URL1 адрес coreapi, а  URL2 адрес usermanagement: 
```console
echo "REACT_APP_BASE_API_URL=\"https://URL1\"" >> .env
echo "REACT_APP_USER_MGT_API_URL=\"https://URL2\"" >> .env
```
- Собрать сборку - `npm run build`
- Скопировать в рабочую директорию папку build. 



###### Инструкция по настройке процессов во flowable (Загрузка bpmn файлов бизнес процессов в lottabyte)

Загрузка bmpn файлов в lottabyte происходит через REST API.
Ниже в примере загрузки предполагается, что LottaByte развернут по адресу 192.168.0.2, пользователь admin, пароль 1234:

1. Получить JWT токен от сервиса авторизации:

```console
curl -X POST \
  https://192.168.0.2/v1/preauth/validateAuth \
  -H 'cache-control: no-cache' \
  -H 'password: 1234' \
  -H 'username: admin'
```
В полученном в ответом сообщении JSON взять данные из поля accessToken.
JWT токен имеет формат "ey....".

2. Загрузить файлы процессов (Lottabyte_One_Step_Approval.bmpn20.xml и Lottabyte_One_Step_Removal.bmpn20.xml находятся в папке bpmn проекта lottabyte) используя полученный ранее JWT токен
```console
curl -X POST \
  'https://192.168.0.2/v1/workflows/repository/deployments?deploymentKey=mainDeployment&deploymentName=mainDeployment' \
  -H 'cache-control: no-cache' \
  -H 'content-type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW' \
  -F 'test=@/home/user/Lottabyte_One_Step_Approval.bpmn20.xml'

curl -X POST \
  'https://192.168.0.2/v1/workflows/repository/deployments?deploymentKey=mainDeployment&deploymentName=mainDeployment' \
  -H 'cache-control: no-cache' \
  -H 'content-type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW' \
  -F 'test=@/home/user/Lottabyte_One_Step_Removal.bpmn20.xml'  
 ``` 
  
###### Запуск сервисов

- Запустить по очереди сервисы back-end
```console
java -jar -Dspring.config.location=classpath:file:coreapi_application.properties coreapi-1.0-SNAPSHOT.jar
java -jar -Dspring.config.location=classpath:file:usermanagement_application.properties usermanagement-1.0-SNAPSHOT.jar
java -jar -Dspring.config.location=classpath:file:scheduler_application.properties scheduler-1.0-SNAPSHOT.jar
```
- Запустить веб-сервер front-end для папки lottabyteui

### Проверка установки

1. Для начала работы с Lottabyte необходимо ознакомиться с [инструкцией по работе с Lottabyte](https://storage.yandexcloud.net/lottabyte-doc/ugLottaByte.1.4.7.pdf).
2. Откройте в браузере сайт front-end  lottabyteui.
3. Учетные данные:
- Имя пользователя : admin 
- пароль : password

### Release notes

## 1.4.0

1. Полностью новый дизайн интерфейса и всех контролов.
2. Стартовая страница с избранным, рекомендуемым и витриной популярного контента.
3. Улучшен переключатель бизнес-режима просмотра контента с минимизацией меню.
4. Расширено отображение свойств объектов в поисковой выдаче.
5. Добавлена функция скрытия бокового меню до иконок.
6. Полностью переработана базовая карточка отображения объекта с вкладками, взаимосвязями и новыми контролами.
7. Переработаны табличные представления объектов с кнопками действий, добавлены колонки и расширены возможности фильтров и сортировки.
8. Доработан функционал внесения расширенного формата описания с форматированием, таблицами, картинками, ссылками (ричтекст) на отдельной вкладке карточки объекта.
9. Переработан интерфейс мэппинга колонок сэмплов данных с атрибутами логических объектов и метаданными.
10. Доработан раздел с метаданными, расширены табличное отображения и добавлены колонки с маппингом.
11. Переработан интерфейс привязки проверок качества данных.
12. Все настройки объединены в одном разделе с вкладками, настройки пользователя централизованы.
13. Добавлено отображение избранного в карточку пользователя, а также разрешений и ролей.
14. Добавлены всплывающие подсказки с отображение статусов и ошибок при завершении действий.
15. Добавлена возможность добавлять в избранное по клику в поисковой выдаче, в таблице со списокм объектов и в карточке объекта.
16. Расширены возможности формирования расписания выполнения задач по загрузуке сэмплов данных и метаданных.

## 1.3.0

1. Добавлена функция отката карточки к любой версии.
2. Добавлен перевод карточек в архивное/активное состояния.
3. Добавлено на линейдж отображение связей ЛО с бизнес-сущностью, продуктом.
4. Добавлено пробрасывание связей на линейдже при скрытии промежуточных объектов.
5. Добавлена функция простановки тегов для разграничения типа взаимовязей на схеме ЛО.
6. Добавлен ```Rich Text``` редактор в атрибуты описания карточек.


## 1.2.0

1. Универсальный контрол (грид) в карточках для отображения всех взаимосвязей карточек каталога.
2. Доработан текста ошибок с уточнением контекста и обработки кода ошибок.
3. Доработанный контрол вывода переменной в формуле показателя со скроллингом.
4. Исправлен баг с редатированием тегов в карточках.
5. Исправлен баг с сохранением изменений в карточках проверок и отображением версий.
6. Расширен атрибутный состав карточек каталога.
7.  Исправлен баг дублирования отображения колонок в мапинге сэпмла с атрибутами логического объекета.
8.  Исправлен баг отображения ссылок в "хлебных крошках".
9. Рассылка уведомлений для администраторов доменов об отработке правил проверки качества данных.
10. Исправлен баг при редактировании атрибуты логического объекта с привязкой систем.
11.  Исправлен баг обвновления линейджа при изменении карточек.
12. В поисковую строку добавлян новый контрол со спецсимволом @ для вывода списка тегов для использования их в качестве фильтров поиска.
13. Добавлена сортировка объектов в списках при редактировании атрибутов типа взаимосвязьv
14. Добавлена проверка на уникальное название системы при редактировании карточек.
15.  Исправлен баг при добавлении домена в настройках пользователя.
16. Доработана визуализация схемы логической модели и инструменты (фильтры с тегами и доменами, массовое выделение объектов с перемещением и др.).
17.  Исправлен баг  при удалении карточки системы с запуском workflow.
18.  Исправлен баг поиска по атрибутам логических объектов.
19. Добавлена возможность прикрепления всех атрибутов логического объекта в один клик при редактировании карточки продукта.
20. Добавлены связи Продукта с Запросом и с самим Продуктом.


## 1.1.0

1. Исправлена ошибка при удалении тега у ЛО и Актива.
2. Доработана схема ЛО, добавлены pk, ссылки на fk, доработана панель со всеми локальными объектами, доработаны инструменты работы с моделью.
3. Доработано запоминание фильтров и сортировок во всех гридах.
4. Доработана фильтрация с тэгами в схеме ЛО.
5. Доработана рассылка для стюардов об ошибках в проверках качества.
6. Доработаны типы проверок DQ.
7. Редактирование всех сущностей теперь выполняется через шаблоны Flowable.
