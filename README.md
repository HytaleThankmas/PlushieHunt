# PlushieHunt

Tracks which hidden plushies each player has found. Each plushie is a trigger
volume with the `FindPlushie` effect. Progress is stored on the player entity as
the `Melodic_PlushiesFound` component, so it is saved with player data and
carries across worlds and restarts. Finding the same plushie again does not
increase the count.

## Trigger volume setup

Add a `FindPlushie` effect to each plushie volume, on the `ENTER` event:

```json
{ "Type": "FindPlushie", "Event": "ENTER", "Plushie": "attic" }
```

| Field | Default | Notes |
| --- | --- | --- |
| `Plushie` | required | Unique ID for this plushie. Without it the effect logs a warning and does nothing. Volume names are not used because they are not kept across server restarts. |
| `FoundMessage` | `You found a hidden plushie! ({found}/{total})` | Sent on a new find. Empty sends nothing. |
| `AlreadyFoundMessage` | `You've already found this plushie. ({found}/{total})` | Sent on a repeat find. Empty sends nothing. |

Messages support `{found}`, `{total}`, and `{plushie}`. Only players are
counted; NPCs entering the volume are ignored.

Other effects on the same volume (sounds, particles) run on every entry. To gate
a reward on progress, add the `PlushiesFound` condition to a volume:

```json
{ "Type": "PlushiesFound", "Event": "ENTER", "Comparison": "AtLeast", "Count": 10 }
```

`Comparison` accepts `AtLeast`, `AtMost`, `Exactly`, `NotEquals`,
`MoreThan`, and `LessThan`. Other spellings make the server skip the condition
with a warning, which leaves the volume ungated.

## Command

`/plushies` shows the player's own progress. It is available to all players.

## Configuration

The first startup creates `mods/Melodic_PlushieHunt/config.json`:

```json
{ "TotalPlushies": 16 }
```

`TotalPlushies` is only used for `{total}` and `/plushies`. Restart after editing.

## Build and install

Targets Hytale 0.7.4. The APIs used here were checked against the local `v0.7.4`
tag. The default build uses the sibling development server JAR. Requires Java 25
or newer and Maven.

```sh
./build.sh
./build.sh -Dhytale.server.jar=/absolute/path/to/Server/HytaleServer.jar
```

Copy `target/PlushieHunt-1.0.0.jar` into the server's `mods` folder, then restart.
