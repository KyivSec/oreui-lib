# Grid layout

`OreGrid` is a client-only native `Layout` with fixed, content-sized and weighted flexible tracks, merged cells and per-cell limits. It does not replace Minecraft's widget/rendering system.

```java
OreGrid grid = new OreGrid(3, 2).gap(8).padding(12)
        .column(0, OreGrid.Track.fixed(100))
        .column(1, OreGrid.Track.flex(1, 80, 400));
grid.add(sidebar, 0, 0, 3, 1, OreGrid.Cell.DEFAULT); // rowSpan, columnSpan
grid.add(title, 0, 1);
grid.add(editor, 1, 1, OreGrid.Cell.DEFAULT.width(80, 300).padding(2));
grid.add(actions, 2, 1);
grid.setSize(480, 300);
grid.setPosition(20, 30);
```

Rows default to content sizing; columns default to equal flex. `Track.fixed`, `Track.content(min,max)` and `Track.flex(weight,min,max)` configure each axis. Flexible tracks share surplus space up to their maxima. Content tracks measure children, while explicit cell minima participate in span sizing. Merged regions include the intervening gaps. Overlapping placements, duplicate ownership, cyclic layouts and invalid limits/spans are rejected.

`Cell.DEFAULT` stretches horizontally, aligns to the top vertically and clips overflow. Its immutable builders configure width/height limits, padding, margin, alignment (`START`, `CENTER`, `END`, `STRETCH`) and overflow (`CLIP`, `VISIBLE`). Center/end alignment provides automatic positioning within remaining cell space, rather than CSS margin collapsing. Padding/margin are explicit nonnegative pixel insets.

Call `setWidth`, `setHeight`, `setSize` or `arrangeElements` to remeasure after content changes. Width is applied before wrapped height is measured. `fitContent()` removes explicit outer dimensions. `getContentWidth/Height` reports occupied tracks; `getWidth/Height` reports the requested outer size when bounded. Minima never silently shrink: if fixed/max-limited tracks or the outer bounds cannot accommodate content, use clipping, visible overflow or a scrolling host. A cell's widget maximum does not force neighboring cells in its shared track to have the same maximum.

Known Ore composite layouts and native `AbstractWidget` children are resized automatically through `OreLayoutSizing`. Unknown native layouts keep their intrinsic dimensions and are positioned/aligned; arrange those layouts yourself before adding them. Fixed-size native spacer elements remain fixed-size. Avoid constraints that contradict an intrinsically square widget's aspect ratio.

## Rendering and input

For position-only layout, register/render child widgets normally. To enforce per-cell clipping, render the grid once and register children for input only:

```java
addRenderableOnly(grid);
grid.visitWidgets(this::addWidget);
```

Do not also register those children as renderables or they will draw twice. A grid recursively draws composite backgrounds and children. Hosts must consult `grid.acceptsInput(widget,x,y)` before dispatching pointer presses/scrolls so clipped areas cannot be clicked. Drag capture may continue outside the cell until release. Traversal follows visual row/column order and visits merged widgets once. Leave padding around controls if their outside focus border must fit inside a clipped cell.

The test gallery's existing sidebar/content geometry is calculated through a grid, retaining the same bounds at narrow and wide GUI sizes.
