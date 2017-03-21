# PI-set-

Protocole de communication tel que je le vois:

 - Client to Serveur :
 
 Prefixe en MAJUSCULE , puis N (le numéro du plateau présent chez le client, pour verification par le serveur) séparateur --> / (assez simle si on garde toujours un seul mot en préfixe)
 liste des commandes:
 - LOGIN/N/nomdelogin
 - NEWGAME/N           --> demande un nouveau plateau
 - POINT/N             --> demande les points (je sais pas si c'est utile)
 - TRY/N/carte1/carte2/carte3 --> propose un set sur le plateau N (considéré que si le serveur est encore a N)
 - SEND --> reste du code récupéré, a priori on s'en sert pas
 - LOGOUT --> je sais pas non plus si on va s'en servir
 - KILL --> permet de tuer le serveur.
 
