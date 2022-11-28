# more on verification 
example of using verification endpoint
```
  "destination": {
    "getMobiles": {
      "verify": {
        "turvis": {
          "method": "post",
          "post_body_struct": "{$incoming_request_body}",
          "endpoint": "https://turvis.ria.ee/"
        },
        "response": {
          "ok": "proceed",
          "nok": "stop"
        }
      },
```
# skip parameter
one can use skip parameter for an endpoint, example using previous snippet
```
     "verify": {
        "turvis": {
          "skip": true,
          "method": "post",
          "post_body_struct": "{$incoming_request_body}",
          "endpoint": "{turvis_url}"
        },
        "response": {
          "ok": "proceed",
          "nok": "stop"
        }
      },
```
This verification endpoint will proceed no matter what, and would be functionally identical to empty verification section
like in the example configuration file (REQUEST_COFIGURATION_EXAMPLE.json.txt). Using skip parameter is convenient for development purposes
as one could skip(or not) endpoints while still keeping relevant endpoint information in them

# more on mapping syntax

In the example configuration (REQUEST_CONFIGURATION_EXAMPLE.json.txt) one can see how mapping from one request to another
is done using dollar sign "$" special character inside curly brackets.
```
      "post_body_struct": {
              "personalCode": "{$.userinfo$.personalCode}"
       },
```
When using hash sign "#" special character in a similar way the mapping is done  directly from incoming post body parameters
that initiated the whole router chain.
```
        "post_body_struct": {
          "tableName": "epost.eaadressid",
          "columnValueMap": {
            "aadress": "{#.email}", //mapped from the body of the original request no matter the nesting level
            "isikukood": "{$.userinfo$.personalCode}",  // mapped from other request
            "aktiveeritud": true
          }
        }
```
There is a convenience descriptor "$incoming_request_body" that you see being used in the first snippet.

```
         "post_body_struct": "{$incoming_request_body}"
```
This special descriptor will just help mapping the whole incoming post body from the original post method.

# array mapping

There is a special construct for a field value used for mapping arrays "#.from_property"

In configuration file the desired output array will be described by writing into it only one
object with fields :  field "#from_property" designating the input array location  and field "properties" that is an 
object containing the fields that one wants to see inside the objects of the output array.
*NB! to designate that the property should be remapped from input array object "#." designator is used*
*It should not be confused with mapping property  from incoming request body*

For example given input
```
{ 
  "emails" : [
              {"email": "some@mail1.com", "note":"somestring1"},
              {"email": "some@mail2.com", "note":"somestring2"}
              ]
}
```
and configuration 

```
          "outArray": [
            {
              "#.from_property": "{#.emails}",
              "properties": {
                "aadress": "{#.email}", //NB!! the #. in this contruct does not refer to incoming body but array node!!
                "isikukood": "{$.userinfo$.personalCode}",
                "aktiveeritud": true
              }
            }
          ]
       
```
we would have output
```
{ 
  "outArray" : [
              {"aadress": "some@mail1.com", "isikukood":"11111111111"},
              {"aadress": "some@mail2.com", "isikukood":"22222222222"}
              ]
}
```
In configuration we decided we did not want "note" field in our output, renamed "email"->"aadress" and added a field 
"isikukood" that has a value determined by previous request. 

# setting response headers

Mock configurations SET_ROLE.json and SET_ROLE_RESTRICTIONS.json provide examples of setting response headers.
Thing of note is the special use of #.from_property construct that was previously described in array mapping context.

Value section in header can be hardcoded as in SET_ROLE_RESTRICTIONS.json or by combined use of  #.from_property
"{$incoming_request_body}" be set from incoming request body. One can also use #.from_property field to map
fields from the "data" field object from input request. 

Example configurations SET_ROLE.json and SET_ROLE_RESTRICTIONS.json are provided and also there are example requests
using these configurations in the REQUEST_EXAMPLES.md. 

For developers -> current logic for adding custom headers is located in addCustomHeaders method in BaseService class.

# using helper functions

Mock configuration RUN_FUNCTION displays different use cases of using functions in configurations. Functions can be used
in "post_body_struct". To designate the start of a function, use `$_`. Everything proceeding the designator up until `(`
will be taken as the name of the function to run. Everything proceeding the opening parenthesis up until `)` will be
taken as the method input (to distinguish separate function inputs, use `$,`)
Example of using helper functions:

```
    "post_body_struct": {
      "desiredLowercase": "$_toLowercase(I WANT THIS TEXT IN LOWERCASE)",
      "desiredEqualsTrue": "$_equals(foo$,foo)",
      "desiredEqualsFalse": "$_equals(foo$,bar)",
    },
```


