Reflect about your solution!

Summary:

Chatserver is limited up to 30 connections simultaneously.
There is only one UDP Packet sent back to the client which contains all available users, so users cant have arbitrary long names.
If the chatserver cut the connection to the clients, they get a "connection lost" message, but dont exit their application.
All messages via tcp or udp are strings, not objects.