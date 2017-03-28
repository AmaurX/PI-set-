# PI-set-

Protocole de communication tel que je le vois:

 --> Client to Serveur :
 
 Prefixe en MAJUSCULE , puis N (le numéro du plateau présent chez le client, pour verification par le serveur) séparateur --> / (assez simle si on garde toujours un seul mot en préfixe) sans espace.
 
 liste des messages:
 - LOGIN/nomdelogin
 - NEWGAME/N                     --> demande un nouveau plateau
 - POINT/N                       --> demande les points (je sais pas si c'est utile)
 - TRY/N/carte1/carte2/carte3    --> propose un set sur le plateau N, je propose de mettre juste l'emplacement (de 0 à 14) des cartes, le serveur connait le plateau (considéré que si le serveur est encore a N)
 - SEND --> reste du code récupéré, a priori on s'en sert pas
 - LOGOUT --> je sais pas non plus si on va s'en servir
 - KILL --> permet de tuer le serveur.
 
--> Serveur to client :

 Prefixe en CamelCase (pour faire la diff), puis N, et les séparateurs en / (sans espace)
 
 liste des messages:
  - TheGame/N/15/63/24/ etc... avec -1 pour les trous, et le numéro de carte entre 0 et 80 inclu
  - ThePoints/N/login1/point1/login2/point2
  - Answer/N/loginasker/answer(true ou false)   --> pour faire l'affichage en rouge ou en vert de la combinaison demandée
  - Welcome LOGIN (reste de l'ancien protocole)
