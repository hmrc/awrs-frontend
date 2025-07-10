#!/bin/bash

curl -XDELETE http://localhost:9595/enrolment-store-stub/data

curl -XPOST http://localhost:9595/enrolment-store-stub/data -H "content-type: application/json" -d '{
		"groupId": "42424200-0000-0000-0000-00000000gaurav2",
  	"affinityGroup": "Organisation",
  	"users": [
  		{
  			"credId": "gaurav123",
  			"name": "Admin",
  			"email": "default@example.com",
  			"credentialRole": "Admin",
  			"description": "User Description"
  		}
  	],
  	"enrolments": [
  		{
  			"serviceName": "HMRC-AWRS-ORG",
  			"identifiers": [
  				{
  					"key": "AWRSRefNumber",
  					"value": "XXAW00000000051"
  				}
  			],
  			"enrolmentFriendlyName": "Awrs Enrolment",
  			"assignedUserCreds": [
  				"api8"
  			],
  			"state": "Activated",
  			"enrolmentType": "principal",
  			"assignedToAll": false
  		}
  	]
  }'

  curl -XPOST http://localhost:9595/enrolment-store-stub/known-facts -H "content-type: application/json" -d '
  {
      "service": "HMRC-AWRS-ORG",
      "knownFacts": [
          {
              "key": "AWRSRefNumber",
              "value": "XXAW00000000051",
              "kfType": "identifier"
          },
          {
              "key": "CTUTR",
              "value": "1111111111",
              "kfType": "verifier"
          },
           {
               "key": "SAUTR",
               "value": "1111111111",
               "kfType": "verifier"
           },
          {
              "key": "Postcode",
              "value": "TF4 3ER",
              "kfType": "verifier"
          }
      ]
  }'