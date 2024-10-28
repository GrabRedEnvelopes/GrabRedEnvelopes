# Grab Red Envelopes

"An online multiplayer matching game implemented in Java."

## Table of Contents

1. [Background](#background)
2. [Features](#features)
3. [Technology Stack](#technology-stack)
4. [Code Overview](#code-overview)
5. [Formula Explanation](#Formula-Explanation)

## Background

"This project aims to provide a user-friendly multiplayer matching system to support the Red Envelopes Grabbing mode, integrated with dynamic points distribution logic."

## Features

- Real-time matching support
- Automatic scheduled tasks and multi-user session management
- Dynamic points allocation to enhance user experience

## Technology Stack

- Java version: 11+
- Framework: Spring Boot, MyBatis, etc.
- Database: MySQL, Redis
- Additional dependencies: WebSocket, JSON parsing libraries, etc.

## Code Overview

In the `RedEnvelopesCore` class, we implement the `EventListener` interface to listen for and process `PlayEvent` events. It primarily handles the card distribution and points allocation logic in the game.

#### Core Logic in `RedEnvelopesCore`

1. **Event Listener Initialization**: Processes events through the `onEvent(PlayEvent playEvent)` method when an event is triggered.

2. **Basic Information Setup**:
    - Retrieves `roomId` and uses `RoomContext.ROOM_MAP` to locate the corresponding room information.
    - Acquires the list of users in the room, `users`, and the game type, `type`.

3. **Generating the Card Flipping Order**:
    - Randomizes the user order and assigns a unique `position` to each user.
    - Generates the flipping order for five rounds, storing each round’s user order in `userOrderMap` with the users rotating each round.

4. **User Holding Settings**:
    - Sets the first user in the first round as the holding user and assigns a holding time.

5. **Random Points Allocation**:
    - Uses the `generateRandomPoints` method to create a random sequence of points, ensuring each card has a point greater than 0.
    - Saves each round's points allocation in `cardPointsOrderMap` for later use.

6. **Data Transmission**:
    - Creates JSON-formatted data containing the current round and user order information.
    - Sends this data to each online user via `WebSocketSession` to ensure real-time updates to users' game statuses.

#### `generateRandomPoints` Method

This method generates a list of random scores that sum to `totalPoints` and ensures each score is greater than 0:

- **Parameters**: `totalPoints` represents the total score, and `numPlayers` represents the number of players.
- **Logic**:
    - Uses a minimum score `minPoint = 1`, distributing points to each player step by step.
    - For each player, calculates the remaining points and maximum allowable points, then randomly allocates points within the range of `minPoint` to `maxAvailable`.
    - Assigns all remaining points to the last player, ensuring the total matches `totalPoints`.

The `RedEnvelopesCore` class provides complete logic for card distribution and point allocation, ensuring fairness and randomness in gameplay.

## Formula Explanation

The goal of this method is to generate a list of random points `points` for multiple players, satisfying the following conditions:

1. **The total sum of points** equals `totalPoints`.
2. **Each point value is greater than zero**.
3. **The last point** contains all remaining points.

#### Step 1: Initialize the Points Pool

- Set the initial remaining points as:
\[
remainingPoints = totalPoints
\]

#### Step 2: Points Distribution Formula

- For each player \( i \) (total of `numPlayers` players):

1. Calculate the upper limit of points `maxPoint`, typically 75% of the current remaining points:
   \[
   maxPoint = 0.75 \times remainingPoints
   \]

2. Calculate the maximum allocatable points for the current player `maxAvailable`:
   \[
   maxAvailable = \min(remainingPoints - (numPlayers - i - 1) \times minPoint, maxPoint)
   \]
   Here, `remainingPoints - (numPlayers - i - 1) * minPoint` ensures enough points are left for the remaining players.

3. Generate the current player's points `point_i`, satisfying the range:
   \[
   minPoint \leq point_i \leq maxAvailable
   \]
   The random points generation formula is:
   \[
   point_i = random(minPoint, maxAvailable)
   \]

4. Update the remaining points:
   \[
   remainingPoints = remainingPoints - point_i
   \]

#### Step 3: Allocate the Last Point

- Allocate all remaining points to the last player:
\[
points[numPlayers - 1] = remainingPoints
\]

---

### Summary Formulas

1. **Initial Points Pool**:
   \[
   remainingPoints = totalPoints
   \]

2. **Each Round Points Formula**:
   \[
   point_i = random(minPoint, \min(remainingPoints - (numPlayers - i - 1) \times minPoint, 0.75 \times remainingPoints))
   \]

3. **Update Remaining Points**:
   \[
   remainingPoints = remainingPoints - point_i
   \]

4. **Points for the Last Player**:
   \[
   points[numPlayers - 1] = remainingPoints
   \]

---

This formula ensures that the total points of all players equal `totalPoints`, and that each point is greater than zero.