# Theme 1 compact furniture correction — 3.0.9

The previous miniature reused the entire expanded bookshelf UI. Ornate trim, brass category buttons, pale timber and strong desaturation remained visible at room scale, giving the appearance of an overlaid UI even after perspective calibration.

The default oak miniature now uses a dedicated, simple square-foot cabinet sprite, generated from the supplied reference and the nine-cell layout. It has no baked-in books. BookshelfView draws actual live book children into measured openings, omitting category controls. Other selected shelf finishes retain their artwork with the same omission of compact category controls. The expanded bookshelf and its controls remain available.

ProjectedCabinetView crossfades the expanded and room snapshots through the existing collapse animation. Room lighting retains wood colour, the side timber is matched to the new oak frame, and both snapshots are released when expanding or destroying the activity. The previously calibrated room projection, foot contacts and lamp interactions remain in use.

Validation includes native Android rendering of empty and populated shelves in daylight and at night, existing landscape/lamp renders, a regression check that category-control changes cannot affect the room snapshot while a saved book does, asset alpha/memory bounds, all unit tests, lint and signed release verification. Native renders are not physical-device validation.
