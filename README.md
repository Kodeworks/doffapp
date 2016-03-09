#
 
Model case classes all needs id:Option[Long] field for slickext macros interaction with DbService

Hyphendata has info on which index a word can be split

For mysql.local database, run create database doffapp default character set latin1;

Whenever a batch of M compound word is split more than N times, the split forms of those words needs to be validated by a human.

Whenever M most used word is used more than N times for discarding words, they must be validated by a human that they actually are unimportant.
Same for TFIDF words.

Need cancellable async classifications