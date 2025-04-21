# PbWarps
This is a simple, admin-defined warp mod for Fabric servers, allowing you to define areas players should have quick access to.
Player's can easily use it by running `/warp NAME` to teleport to selected warp or just run `/warp` to open an ui to select
from all available ones.

Warps can have a custom formatted name, icons, positions/rotation and predicate, limiting who can use it.
This mod has support for using polymer resource packs for nicer looking ui.

![](https://i.imgur.com/IEzKZQB.png)

## Admin Usage.
All admin commands are found under `/warps` command.

To create warp, you use `/warps create <id>`. You can also supply multiple additional parameters to configure it.

You can also modify existing warps by using `/warps modify <id> ...` and all of it's subcommands.

The predicates use Predicate API, for which format is described here: https://github.com/Patbox/PredicateAPI/blob/1.21.2/BUILTIN.md
Only difference being, that the predicate type is passed as it's own argument before predicate data.

You can display info about the warp by using `/warps info <id>` or remove the warp with `/warps remove <id>` command.

To teleport other players or to skip the predicate checks, you can use `/warps teleport <id>` command.