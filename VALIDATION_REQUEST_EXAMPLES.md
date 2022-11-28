Valideerimisteenus võimaldab võrrelda kahte andmeobjekti.
Valideerimisteenusele tehtava päringu JSON sisaldab järgmisi välju: 
"input" - andmeobjekt mida tahetakse valideerida
"validate_against" - võrdlus andmeobjekt mille vastu valideerimine toimub
"validation_type" - muutuja mis täpsustab kuidas valideerimine toimub
Praegusel hetkel on meil "validation_type" jaoks kolm korrektset sisendit:
"MUST_MATCH_ALL" - Kontrollitakse, et kõik "input" andmeväljaga kaasa pandud väärtused sisalduvad ka "validate_against" hulgas, mitte vastupidi.
"MUST_MATCH_ANY" - Kontrollitakse, et vähemalt 1 "input" andmeväljaga kaasa pandud väärtustest sisaldub ka "validate_against" hulgas.
"MUST_MATCH_EXACT" - Andmeväljade "input" ja "validate_against" sisu peavad olema identsed, st. lubatud ei ole ka samade andmete esitamine teistesuguses järjekorras


Näited vastavatest päringutest:

#MUST_MATCH_ALL

``curl -X POST -H "Content-Type: application/json" -d '{ "input": { "registrikood": "1234567890" }, "validate_against": { "registrikood": ["1234567890", "0123456789"] },  "validation_type": "MUST_MATCH_ALL" }' http://localhost:8080/match_input``
Oodatav vastus (vastuse  HTTP kood on 200)
true

``curl -X POST -H "Content-Type: application/json" -d '{ "input": { "registrikood": ["1234567890", "0123456789"] }, "validate_against": { "registrikood": "1234567890" }, "validation_type": "MUST_MATCH_ALL" }' http://localhost:8080/match_input``
Oodatav vastus (vastuse HTTP kood on 401)
false


#MUST_MATCH_ANY

``curl -X POST -H "Content-Type: application/json" -d '{ "input": { "registrikood": "1234567890" }, "validate_against": { "registrikood": ["1234567890", "0123456789"] }, "validation_type": "MUST_MATCH_ANY" }' http://localhost:8080/match_input``
Oodatav vastus (vastuse  HTTP kood on 200)
true

``curl -X POST -H "Content-Type: application/json" -d '{ "input": { "registrikood": "9999999234" }, "validate_against": { "registrikood": ["1234567890", "0123456789"] }, "validation_type": "MUST_MATCH_ANY" }' http://localhost:8080/match_input``
Oodatav vastus (vastuse HTTP kood on 401)
false

#MUST_MATCH_EXACT

``curl -X POST -H "Content-Type: application/json" -d '{ "input": { "registrikood": ["1234567890", "0123456789"] }, "validate_against": { "registrikood": ["1234567890", "0123456789"] }, "validation_type": "MUST_MATCH_EXACT" }' http://localhost:8080/match_input``
Oodatav vastus (vastuse  HTTP kood on 200)
true

``curl -X POST -H "Content-Type: application/json" -d '{ "input": { "registrikood": ["1234567890", "0123456789"] }, "validate_against": { "registrikood": "1234567890" }, "validation_type": "MUST_MATCH_EXACT" }' http://localhost:8080/match_input``
Oodatav vastus (vastuse HTTP kood on 401)
false
