# 6.0.1

## Fixed

- **Tree Capitator no longer destroys a neighbouring tree's canopy.** Leaf ownership now uses vanilla's own `LeavesBlock.DISTANCE` value: a leaf is only removed when the closest felled log is exactly as near as the distance vanilla recorded when the tree grew. A leaf whose nearest log belongs to another tree is left standing instead of being swept up by a simple radius check.
- **Tree Capitator works again in dense forests.** When every leaf near a tree was also within range of a neighbouring tree's log, the canopy filter left no leaves at all and the whole tree was skipped, including its logs. The connected logs are now always felled; the filter only decides which leaves are removed. The leaf search now covers vanilla's full 6-block decay range (was 3), so outer leaves no longer hang in the air, and the safety cap was raised so large trees are not cut short.
