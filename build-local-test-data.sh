#!/bin/bash

#curl -XDELETE http://localhost:9595/enrolment-store-stub/data

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
  			"serviceName": "IR-SA",
  			"identifiers": [
  				{
  					"key": "UTR",
  					"value": "1111111111"
  				}
  			],
  			"enrolmentFriendlyName": "IR SA Enrolment",
  			"assignedUserCreds": [
  				"api8"
  			],
  			"state": "Activated",
  			"enrolmentType": "principal",
  			"assignedToAll": false
  		}
  	]
  }'