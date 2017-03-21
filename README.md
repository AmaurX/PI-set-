# PI-set-

Protocole de communication tel que je le vois:

 --> Client to Serveur :
 
 Prefixe en MAJUSCULE , puis N (le numéro du plateau présent chez le client, pour verification par le serveur) séparateur --> / (assez simle si on garde toujours un seul mot en préfixe) sans espace.
 liste des messages:
 - LOGIN/N/nomdelogin
 - NEWGAME/N           --> demande un nouveau plateau
 - POINT/N             --> demande les points (je sais pas si c'est utile)
 - TRY/N/carte1/carte2/carte3 --> propose un set sur le plateau N, je propose de mettre juste l'emplacement (de 0 à 14) des cartes, le serveur connait le plateau (considéré que si le serveur est encore a N)
 - SEND --> reste du code récupéré, a priori on s'en sert pas
 - LOGOUT --> je sais pas non plus si on va s'en servir
 - KILL --> permet de tuer le serveur.
 
--> Serveur to client :

 Prefixe en CamelCase (pour faire la diff), puis N, et les séparateurs en / (sans espace)
 liste des messages:
  - TheGame/15/63/24/ etc... avec -1 pour les trous, et le numéro de carte entre 0 et 80 inclu
  - ThePoints/login1/point1/login2/point2 
  - Welcome LOGIN (reste de l'ancien protocole)
