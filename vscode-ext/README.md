# Modeling language for Interface Automata

## Getting started

1. Sample source code: `vending.ia`

```
actions { coin, relT }
var c: int
var v: int
proc P(input: int) {
    act { coin?, relT! }
    while {
        v < 1 && c == input -> {
            coin?
            guarantee(v' > v)
        }
        v >= 1 -> {
            relT!
            assume(v' == v)
            break
        }
    }
}
init {
    sys p1 = P(1)
    sys p2 = P(2)
}
```

## Supported command
- IA Toolset
  + Run
  + Debug
  + Interactive alternating simulation
  + Reload IA IDE Server