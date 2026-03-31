

internal object Responses {

    val emptyTickets = """
        {
            "applications": [
                {
                    "app_id": "xZlr1Zf0pZZE43NfjXfY10OvEKwkKLRCO~PYF7SjID-Tp-7sK5EAuWqgOfrCQNOdDUHrZhHlBaqcdzj2ULgf9e~ciFudXo9ff1Y9cx0oXaTGziZKANoCLbWceaF-5g1VAQpfcg==",
                    "org_name": "Droid 3",
                    "org_logo_url": "/mobilelogo?p=xZlr1Zf0pZZE43NfjXfY10OvEKwkKLRCO%7EPYF7SjID-Tp-7sK5EAuWqgOfrCQNOdDUHrZhHlBaqcdzj2ULgf9e%7EciFudXo9ff1Y9cx0oXaTGziZKANoCLbWceaF-5g1VAQpfcg%3D%3D&v=1030371235",
                    "org_description": "",
                    "welcome_message": "can I help you, <a href=\"https://pyrus.com/t#fc1525082/mobile-app-chat\">https://pyrus.com/t#fc1525082/mobile-app-chat</a>?<br><br><a href=\"https://google.com\">link</a><br><a data-type=\"button\" href=\"https://pyrus.com/t#fc1525082/mobile-app-chat\">no</a><button>yes</button>",
                    "rating_settings": {
                        "size": 2,
                        "type": 2
                    }
                }
            ]
        }
    """.trimIndent()

    val markTicketIsRead = """
        {
          "tickets": [
            {
              "ticket_id": 1,
              "subject": "test",
              "author": "",
              "is_read": true,
              "last_comment": {
                "comment_id": 1234567890,
                "body": "testComment",
                "created_at": "2026-03-31T13:09:14Z",
                "is_system": false,
                "system_comment_type": 0
              },
              "is_active": true,
              "created_at": "2026-03-12T13:30:46Z",
              "show_rating": false,
              "last_read_comment_id": 1234567890
            }
          ],
          "applications": [
            {
              "app_id": "xZlr1Zf0pZZE43NfjXfY10OvEKwkKLRCO~PYF7SjID-Tp-7sK5EAuWqgOfrCQNOdDUHrZhHlBaqcdzj2ULgf9e~ciFudXo9ff1Y9cx0oXaTGziZKANoCLbWceaF-5g1VAQpfcg==",
              "org_name": "Droid 2",
              "org_logo_url": "/mobilelogo?p=xZlr1Zf0pZZE43NfjXfY10OvEKwkKLRCO%7EPYF7SjID-Tp-7sK5EAuWqgOfrCQNOdDUHrZhHlBaqcdzj2ULgf9e%7EciFudXo9ff1Y9cx0oXaTGziZKANoCLbWceaF-5g1VAQpfcg%3D%3D&v=1030371235",
              "org_description": "",
              "welcome_message": "can I help you, <a href=\"https://pyrus.com/t#fc1525082/mobile-app-chat\">https://pyrus.com/t#fc1525082/mobile-app-chat</a>?<br><br><a href=\"https://google.com\">link</a><br><a data-type=\"button\" href=\"https://pyrus.com/t#fc1525082/mobile-app-chat\">no</a><button>yes</button>",
              "rating_settings": {
                "size": 2,
                "type": 2
              }
            }
          ],
          "commands_result": [
            {
              "command_id": "a16c1c7c-a301-4eae-b9de-b47816b42533",
              "ticket_id": 1,
              "comment_id": 1234567890
            }
          ]
        }
    """.trimIndent()

    val setPushTokenResponse = """
        {
          "tickets": [
            {
              "ticket_id": 1,
              "subject": "test",
              "author": "",
              "is_read": true,
              "last_comment": {
                "comment_id": 1234567890,
                "body": "testComment",
                "created_at": "2026-03-31T13:09:14Z",
                "is_system": false,
                "system_comment_type": 0
              },
              "is_active": true,
              "created_at": "2026-03-12T13:30:46Z",
              "show_rating": false,
              "last_read_comment_id": 1234567890
            }
          ],
          "applications": [
            {
              "app_id": "xZlr1Zf0pZZE43NfjXfY10OvEKwkKLRCO~PYF7SjID-Tp-7sK5EAuWqgOfrCQNOdDUHrZhHlBaqcdzj2ULgf9e~ciFudXo9ff1Y9cx0oXaTGziZKANoCLbWceaF-5g1VAQpfcg==",
              "org_name": "Droid 2",
              "org_logo_url": "/mobilelogo?p=xZlr1Zf0pZZE43NfjXfY10OvEKwkKLRCO%7EPYF7SjID-Tp-7sK5EAuWqgOfrCQNOdDUHrZhHlBaqcdzj2ULgf9e%7EciFudXo9ff1Y9cx0oXaTGziZKANoCLbWceaF-5g1VAQpfcg%3D%3D&v=1030371235",
              "org_description": "",
              "welcome_message": "can I help you, <a href=\"https://pyrus.com/t#fc1525082/mobile-app-chat\">https://pyrus.com/t#fc1525082/mobile-app-chat</a>?<br><br><a href=\"https://google.com\">link</a><br><a data-type=\"button\" href=\"https://pyrus.com/t#fc1525082/mobile-app-chat\">no</a><button>yes</button>",
              "rating_settings": {
                "size": 2,
                "type": 2
              }
            }
          ],
          "commands_result": [
            {
              "command_id": "3ec22d08-1ae1-475a-a36d-e96ffbc54e74"
            }
          ]
        }
    """.trimIndent()

    val createComment = """
        {
          "tickets": [
            {
              "ticket_id": 1,
              "subject": "test",
              "author": "",
              "is_read": true,
              "last_comment": {
                "comment_id": 1234567890,
                "body": "testComment",
                "created_at": "2026-03-31T13:09:14Z",
                "is_system": false,
                "system_comment_type": 0
              },
              "is_active": true,
              "created_at": "2026-03-12T13:30:46Z",
              "show_rating": false,
              "last_read_comment_id": 1234567890
            }
          ],
          "applications": [
            {
              "app_id": "xZlr1Zf0pZZE43NfjXfY10OvEKwkKLRCO~PYF7SjID-Tp-7sK5EAuWqgOfrCQNOdDUHrZhHlBaqcdzj2ULgf9e~ciFudXo9ff1Y9cx0oXaTGziZKANoCLbWceaF-5g1VAQpfcg==",
              "org_name": "Droid 2",
              "org_logo_url": "/mobilelogo?p=xZlr1Zf0pZZE43NfjXfY10OvEKwkKLRCO%7EPYF7SjID-Tp-7sK5EAuWqgOfrCQNOdDUHrZhHlBaqcdzj2ULgf9e%7EciFudXo9ff1Y9cx0oXaTGziZKANoCLbWceaF-5g1VAQpfcg%3D%3D&v=1030371235",
              "org_description": "",
              "welcome_message": "can I help you, <a href=\"https://pyrus.com/t#fc1525082/mobile-app-chat\">https://pyrus.com/t#fc1525082/mobile-app-chat</a>?<br><br><a href=\"https://google.com\">link</a><br><a data-type=\"button\" href=\"https://pyrus.com/t#fc1525082/mobile-app-chat\">no</a><button>yes</button>",
              "rating_settings": {
                "size": 2,
                "type": 2
              }
            }
          ],
          "commands_result": [
            {
              "command_id": "b5c9c6b0-5584-4f6b-a8c4-0017998db26e",
              "ticket_id": 1,
              "comment_id": 1234567890
            }
          ]
        }
    """.trimIndent()

    val createCommentError = """
        {
          "tickets": [
            {
              "ticket_id": 1,
              "subject": "test",
              "author": "",
              "is_read": true,
              "last_comment": {
                "comment_id": 1234567890,
                "body": "testComment",
                "created_at": "2026-03-31T13:09:14Z",
                "is_system": false,
                "system_comment_type": 0
              },
              "is_active": true,
              "created_at": "2026-03-12T13:30:46Z",
              "show_rating": false,
              "last_read_comment_id": 1234567890
            }
          ],
          "applications": [
            {
              "app_id": "xZlr1Zf0pZZE43NfjXfY10OvEKwkKLRCO~PYF7SjID-Tp-7sK5EAuWqgOfrCQNOdDUHrZhHlBaqcdzj2ULgf9e~ciFudXo9ff1Y9cx0oXaTGziZKANoCLbWceaF-5g1VAQpfcg==",
              "org_name": "Droid 2",
              "org_logo_url": "/mobilelogo?p=xZlr1Zf0pZZE43NfjXfY10OvEKwkKLRCO%7EPYF7SjID-Tp-7sK5EAuWqgOfrCQNOdDUHrZhHlBaqcdzj2ULgf9e%7EciFudXo9ff1Y9cx0oXaTGziZKANoCLbWceaF-5g1VAQpfcg%3D%3D&v=1030371235",
              "org_description": "",
              "welcome_message": "can I help you, <a href=\"https://pyrus.com/t#fc1525082/mobile-app-chat\">https://pyrus.com/t#fc1525082/mobile-app-chat</a>?<br><br><a href=\"https://google.com\">link</a><br><a data-type=\"button\" href=\"https://pyrus.com/t#fc1525082/mobile-app-chat\">no</a><button>yes</button>",
              "rating_settings": {
                "size": 2,
                "type": 2
              }
            }
          ],
          "commands_result": [
            {
              "command_id": "b5c9c6b0-5584-4f6b-a8c4-0017998db26e",
              "error": {
                "text": "Access Denied",
                "code": 0
              }
            }
          ]
        }
    """.trimIndent()

    val calcOperatorTime = """
        {
          "tickets": [
            {
              "ticket_id": 1,
              "subject": "test",
              "author": "",
              "is_read": true,
              "last_comment": {
                "comment_id": 1234567890,
                "body": "testComment",
                "created_at": "2026-03-31T13:09:14Z",
                "is_system": false,
                "system_comment_type": 0
              },
              "is_active": true,
              "created_at": "2026-03-12T13:30:46Z",
              "show_rating": false,
              "last_read_comment_id": 1234567890
            }
          ],
          "applications": [
            {
              "app_id": "xZlr1Zf0pZZE43NfjXfY10OvEKwkKLRCO~PYF7SjID-Tp-7sK5EAuWqgOfrCQNOdDUHrZhHlBaqcdzj2ULgf9e~ciFudXo9ff1Y9cx0oXaTGziZKANoCLbWceaF-5g1VAQpfcg==",
              "org_name": "Droid 2",
              "org_logo_url": "/mobilelogo?p=xZlr1Zf0pZZE43NfjXfY10OvEKwkKLRCO%7EPYF7SjID-Tp-7sK5EAuWqgOfrCQNOdDUHrZhHlBaqcdzj2ULgf9e%7EciFudXo9ff1Y9cx0oXaTGziZKANoCLbWceaF-5g1VAQpfcg%3D%3D&v=1030371235",
              "org_description": "",
              "welcome_message": "can I help you, <a href=\"https://pyrus.com/t#fc1525082/mobile-app-chat\">https://pyrus.com/t#fc1525082/mobile-app-chat</a>?<br><br><a href=\"https://google.com\">link</a><br><a data-type=\"button\" href=\"https://pyrus.com/t#fc1525082/mobile-app-chat\">no</a><button>yes</button>",
              "rating_settings": {
                "size": 2,
                "type": 2
              }
            }
          ],
          "commands_result": [
            {
              "command_id": "b5c9c6b0-5584-4f6b-a8c4-0017998db261",
              "ticket_id": 1,
              "operator_response_time_message": "подождите 5 минут"
            }
          ]
        }
    """.trimIndent()

}