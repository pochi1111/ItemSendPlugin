﻿# ItemSendPlugin
## このプラグインはDB(Mysql)を使用します
構造
id(自動で決めてくれるやつ)
sender_name(TEXT)
sendto_name(TEXT)
item1(TEXT)
item2(TEXT)
...
item8(TEXT)
givedcheck(TEXT)
のようにしてください
作成コマンドは
```sql
create table items
(
    id          int auto_increment
        primary key,
    sender_name text null,
    sendto_name text null,
    item1       text null,
    item2       text null,
    item3       text null,
    item4       text null,
    item5       text null,
    item6       text null,
    item7       text null,
    item8       text null,
    givedcheck  text null
)
    charset = utf8;
```
./itemsend ユーザー名 発送
./itemreceive 受け取り

mysqlの設定はconfig.ymlから行ってください
