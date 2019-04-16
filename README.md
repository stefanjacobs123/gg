# GG

This project aims to provide a solution to the Goose Game. 

The project name "GG" is a wordplay (pun not intended) on "good game" & "goose game".

## Implementation

This implementation will focus only on running one game at a time.
 
The game is implemented inside an actor. To run multiple games would therefore require little modification. 

### Restrictions

The following restrictions were placed on the game as to simplify the implementation:

1. A player can be added anytime during play.
2. A player can't be removed.
3. Players can cheat and play out of turn.
4. A dice can only have values 1 to 6.
5. The game is played with two die.
6. A player name can't contain spaces - will remove spaces.
7. To roll the die manually, it has to be in the format "5, 6" (space after the comma).

### Future improvements

1. Tests. 
2. Create a better cmdline interface - less finicky. 
2. Investigate ways of not having business logic in `com.br.BoardGameActor.Move`'s toString method.
3. Better error-handling.

How to handle player that d

## Run

`sbt run`
