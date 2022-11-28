Need näited sõltuvad õigel viisil seadistatud näidiskonfiguratsioonifailidest. See tekst siin on kirjutatud eeldusega,
et seda hoitakse ajakohastatuna seoses muudatustega repositooriumi /mock_configurations kataloogis olevate failidega.

Konfiguratsioonid kasutavad rakenduse näidis teenuste otspunkte. 
/sandbox/ok ja sandbox/nok

Vastustes olevad väljad "uiIn" ja "uiOut" väljad näitavad päringu/vastuse päises olevat päringu ID-d.
Antud hetkel on käesoleva rakenduse teenuste otspunktid kaasa arvatud sandbox otspunktid seadistatud lisama vastuste päisesse
unikaalse ID, mis kas võetakse vastuse tekistanud päringu päisest või juhul kui see päringu päises puudus siis genereeritakse koha peal.
Seega oleks oodatavad väärtused vastuses "uiIn" ja "uiOut" puhul kas null ja mingi ID või siis kaks ühesugust ID väärtust.
"endpoint" väli näitab päritud teenuse nime. "postBody" näitab teenusele suunatud POST päringu kehas olevaid andmeid. (Pmst ."peegeldab" 
POST päringusse antud sisend parameetreid tagasi.) See annab võimaluse demonstreerida ühe päringu vastuse kasutamist
teise päringu sisuks

Jälgi kuidas ruuter tagastab vastuseid kasutades struktuuri {"data": {}, "error":null}. 

Arendajatele: See struktuur on määratud RUUTER_BASE_RESPONSE_STRUCTURE nimelise konstandiga.

Jälgi kuidas ka päringutel, mis peatuvad vigadest teavitavate http-staatuste tõttu või peaksid edastama veateteid,  on vastuste http-staatuseks
siiski 200. Tegemist on turvameetmega.



#TWO_OK.json

```curl -X POST -H "Content-Type: application/json"  http://localhost:8080/mock/TWO_OK```
oodatud väljund
```{"data":{"TWO_OK":{"endpoint":"postOK","uiIn":"1571743815138","uiOut":"1571743815138","postBody":{"data":{"endpoint":"getOK","uiIn":"null","uiOut":"1571743815079"}}}},"error":null}```

Demonstreeritakse konfiguratsiooni, kus sissetuleva päringule vastuse saamiseks teeb ruuter päringu kahele erinevale teenusele.
Mõlemad päringud õnnestuvad, kusjuures ühe päringu vastust kasutatakse järgmise päringu sisendina.

#TWO_OK_PARTIAL.json

``` curl -X POST -H "Content-Type: application/json"  http://localhost:8080/mock/TWO_OK_PARTIAL```
oodatud väljund
```{"data":{"TWO_OK":{"endpoint":"postOK","uiIn":"1571743815138","uiOut":"1571743815138","postBody":{"endpoint":"getOK"}}},"error":null}```
Nagu eelmine kuid demonstreerib kuidas on võimalik ühe päringu vastust ainult OSALISELT kasutada teise päringu sisendina

#INNER_STOP.json
```curl -X POST -H "Content-Type: application/json"  http://localhost:8080/mock/INNER_STOP```
oodatud väljund
```{"data":{},"error":null}```
Sisemise päringu HTTP-400 vastuse korral edasised päringud peatatakse. Vastuse "data" sektsiooni midagi ei kirjutata
Staatus sellisel vastusel on ikkagi 200.

#INNER_IGNORE.json

```curl -X POST -H "Content-Type: application/json"  http://localhost:8080/mock/INNER_IGNORE```
oodatud väljund
```{"data":{"TWO_OK":{"endpoint":"getNOK","uiIn":"1571743815138","uiOut":"1571743815138"}},"error":null}```
Ignoreeritakse fakti et sisemine päring sai HTTP 400 fakti, päringuid tehakse edasi. Vastuse "data" sektsiooni kirjutatakse
vastuse keha sisu. Staatus ka sellisel vastusel on 200.

#INNER_ON_ERROR.json

```curl -X POST -H "Content-Type: application/json"  http://localhost:8080/mock/INNER_ON_ERROR```
oodatud väljund
```{"data":{},"error":{"TWO_OK":{"endpoint":"getNOK","uiIn":"1571743815138","uiOut":"1571743815138"}}}```
Päringuid tehakse edasi kuid sisemise HTTP 400 päringu vastus kopeeritakse vastuse "error": osasse.
Staatus sellisel vastusel on ikkagi 200.

#OUTER_STOP.json

```curl -v  -X POST -H "Content-Type: application/json"  http://localhost:8080/mock/OUTER_STOP```
oodatud väljund
tühja kehaga vastus mille staatuseks siiski HTTP 200

Sellest näites on sisemise  päingu vastuse puhul seatud "proceed" hoolimata sellest kas vastusel kas on õnnestumist
või viga märkiv http staatus. (Sisemine päring annab 400 koodiga  vastuse). Päring jätkub kuni välimise sõlmeni kus on seatud et NOK vastuste
puhul tehakse "stop".  
400 koodiga vastuse tõttu peatatakse nüüd kõige välimise vastuse koostamine ja saadetakse tühi vastus.

NB! kõik ülalpool toodud näited kus on räägitud päringu vastusest 400 võiks selle asendada ka mõne muu
http veakoodiga 4XX või 5XX seast.

#FORWARD.json

```curl  -X POST -H "Content-Type: application/json"  http://localhost:8080/mock/FORWARD```
oodatud väljund
```{"data":{"FORWARD":{"endpoint":"getOK","uiIn":"1571743815138","uiOut":"1571743815138"}},"error":null}```
lihtne päringu edasisuunamine

#SET_ROLE.json
``curl -v  -X POST -H "Content-Type: application/json" -d '{ "ID": "TEST11111111", "name": "KASUTAJA DEMO", "role": "i18n_role_citizen"}'  http://localhost:8080/mock/SET_ROLE``
oodatud vastus
``` 
  > POST /mock/SET_ROLE HTTP/1.1
  > Host: localhost:8080
  > User-Agent: curl/7.58.0
  > Accept: */*
  > Content-Type: application/json
  > Content-Length: 78
  > 
  * upload completely sent off: 78 out of 78 bytes
  < HTTP/1.1 200 
  < Access-Control-Allow-Credentials: true
  < Access-Control-Allow-Methods: POST, GET
  < Access-Control-Max-Age: 3600
  < REQUEST_UID: 1571917243108
  < Set-Cookie: CUSTOMJWT=%7B%22ID%22%3A%22TEST11111111%22%2C%22name%22%3A%22KASUTAJA+DEMO%22%2C%22role%22%3A%22i18n_role_citizen%22%7D; Max-Age=300000; Expires=Sun, 27-Oct-2019 23:00:43 GMT; Domain=arendus.eesti.ee; Path=/; Secure
  < Pragma: no-cache
  < Cache-Control: no-cache
  < Expires: Wed, 31 Dec 1969 23:59:59 GMT
  < Content-Type: application/json;charset=UTF-8
  < Transfer-Encoding: chunked
  < Date: Thu, 24 Oct 2019 11:40:43 GMT
  < 
  * Connection #1 to host localhost left intact
  {"data":{"SET_ROLE_RESTRICTIONS":{"endpoint":"postOK","uiIn":"1571917243091","uiOut":"1571917243091","postBody":{}}},"error":null}
  ```
post päringu andmed asetatakse set-cookie headeri sisuks

#SET_ROLE_RESTRICTIONS.json
``curl -v POST -H "Content-Type: application/json" -d '{ "ID": "TEST11111111", "name": "KASUTAJA DEMO", "role": "i18n_role_citizen"}'  http://localhost:8080/mock/SET_ROLE_RESTRICTIONS``
oodatud vastus
```
> POST /mock/SET_ROLE_RESTRICTIONS HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.58.0
> Accept: */*
> Content-Type: application/json
> Content-Length: 78
> 
* upload completely sent off: 78 out of 78 bytes
< HTTP/1.1 200 
< Access-Control-Allow-Credentials: true
< Access-Control-Allow-Methods: POST, GET
< Access-Control-Max-Age: 3600
< REQUEST_UID: 1571917385399
< Set-Cookie: role_list=%7B%22read_only%22%3A%7B%22i18n_role_citizen%22%3A%5B%7B%22name%22%3A%22KASUTAJA+DEMO%22%2C%22ID%22%3A%22TEST11111111%22%7D%5D%2C%22i18n_role_parent%22%3A%5B%7B%22name%22%3A%22Esimene+Poeg%22%2C%22ID%22%3A%22334445555%22%7D%2C%7B%22name%22%3A%22Poeg+II%22%2C%22ID%22%3A%22554445555%22%7D%2C%7B%22name%22%3A%22T%C3%BCtar+Laps%22%2C%22ID%22%3A%22778889999%22%7D%5D%2C%22i18n_role_organisations%22%3A%5B%7B%22name%22%3A%22Test+O%C3%9C%22%2C%22ID%22%3A%22111111111%22%7D%2C%7B%22name%22%3A%22Test2+O%C3%9C%22%2C%22ID%22%3A%2222222222%22%7D%2C%7B%22name%22%3A%22Riigi+Infos%C3%BCsteemi+Amet%22%2C%22ID%22%3A%2212345677%22%7D%2C%7B%22name%22%3A%22Test+Kasutaja%22%2C%22ID%22%3A%2233333333%22%7D%5D%7D%7D; Max-Age=300000; Expires=Sun, 27-Oct-2019 23:03:05 GMT; Domain=arendus.eesti.ee; Path=/; Secure
< Pragma: no-cache
< Cache-Control: no-cache
< Expires: Wed, 31 Dec 1969 23:59:59 GMT
< Content-Type: application/json;charset=UTF-8
< Transfer-Encoding: chunked
< Date: Thu, 24 Oct 2019 11:43:05 GMT
< 
* Connection #1 to host localhost left intact
{"data":{"SET_ROLE_RESTRICTIONS":{"endpoint":"postOK","uiIn":"1571917385383","uiOut":"1571917385383","postBody":{}}},"error":null}

```
vastusesse lisatakse konfiguratsioonis seadistatud set-cookie header

#REMOVE_COOKIE.json
```curl -v -X POST -H "Content-Type: application/json"  http://localhost:8080/mock/LOGOUT```
oodatud vastus
```
> POST /mock/LOGOUT HTTP/1.1
  > Host: localhost:8080
  > User-Agent: curl/7.58.0
  > Accept: */*
  > Content-Type: application/json
  > 
  < HTTP/1.1 200 
  < Access-Control-Allow-Credentials: true
  < Access-Control-Allow-Methods: POST, GET
  < Access-Control-Max-Age: 3600
  < REQUEST_UID: 1572852432072
  < Set-Cookie: token=null; Max-Age=0; Expires=Thu, 01-Jan-1970 00:00:10 GMT; Domain=arendus.eesti.ee; Path=/
  < Pragma: no-cache
  < Cache-Control: no-cache
  < Expires: Wed, 31 Dec 1969 23:59:59 GMT
  < Content-Type: application/json;charset=UTF-8
  < Transfer-Encoding: chunked
  < Date: Mon, 04 Nov 2019 07:27:12 GMT
  < 
  * Connection #0 to host localhost left intact
  {"data":{"LOGOUT":{"endpoint":"postOK","uiIn":"1572852432051","uiOut":"1572852432051","postBody":{}}},"error":null}
```
vastus sisaldab headerit mis paneb browserit eemaldama cookie-t nimega "token".
Kui konfiguratsioonis kirjutada maxAge:0 siis seab see Set-Cookie headeris väärtuse
Expires=Thu, 01-Jan-1970 00:00:10 GMT. Kui expires väärtus on minevikus siis enamik
tänapäeva browsereid selle peale eemaldavad cookie. Juhuks kui tegemist on mõne sellise browseriga
mis niimoodi ei talita on konfiguratsioonis seatud ka cookie's väärtus "token" tühjaks.


#ARRAY_MAPPING.json
```
curl -v POST -H "Content-Type: application/json" -d '{ "emails": [ {"email": "multi1@tst.com", "salajane" : "privmes1"}, {"email": "multi2@tst.com",  "salajane" : "privmes2"}]}'  http://localhost:8080/mock/ARRAY_MAPPING
```
oodatud vastus

```
{"data":{"TWO_OK":{"endpoint":"postOK","uiIn":"1583146457763","uiOut":"1583146457763","postBody":{"tableName":"epost.eaadressid","columnValueMaps":[{"aadress":"multi1@tst.com","aktiveeritud":true},{"aadress":"multi2@tst.com","aktiveeritud":true}]}}},"error":null}
```

#VERIFY_SUCCESS ja VERIFY_FAILURE
need on kaks muidu samasugust konfiguratsiooni mis demonstreerivad verifitseerimist
ning vastavalt tulemusele erinevate järgnevate päringute tegemist
SUCCESS konfiguratsioonis on kasutatud verifitseerimise tegemiseks alati https-staatusega 200 vastavat
API otsa ja FAILURE puhul alati http-staatusega 400 vastavat API otsa.

päringud 

```
curl -v POST http://localhost:8080/mock/VERIFY_SUCCESS
curl -v POST http://localhost:8080/mock/VERIFY_FAILURE
```

oodatud vastused

```
{"data":{"VERIFY_SUCCESS":{"endpoint":"getOK","uiIn":"1585116172590","uiOut":"1585116172590"}},"error":null}

{"data":{},"error":null}
```

#CHAIN_REQUEST

päring

```
curl POST  http://localhost:8080/mock/CHAIN_REQUEST
```

oodatud vastus

```
{"data":{"thirdPost":{"endpoint":"postOK","uiIn":"1587551056217","uiOut":"1587551056217","postBody":{"firstReqInfo":"I was posted in first request","secondReqInfo":"I was posted in second request"}}},"error":null}
```

kasutades massiivi struktuuri destinations väljal on meil lisandunud uus viis ühtede päringute
tulemust teiste päringute sisendina kasutada.
näidiskonfiguratsioonis kasutatakse viimases päringus kahe eelmise päringu tulemust ridades

```
"firstReqInfo": "{#.firstPost#.postBody#.stuff1}",
"secondReqInfo" :  "{#.secondPost#.postBody#.stuff2}"
```
            
Märkida tuleb, et esimese "#." järgne tekst sisaldab eelmise päringu nime (massiivi elemendi esimese välja nimi)
järgmised read sisaldavad vastava päringu väljundi json struktuuri. "postBody" on nt. on kõrgeima taseme väli
eelmiste päringute json vastuses.
