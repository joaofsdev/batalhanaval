import { memo } from 'react';
import { SHIP_SPRITES } from '../../constants/ship-sprites';

const CELL_SIZE = 32;
const GAP_SIZE = 1;

const ShipSprite = memo(
  ({ shipType, originRow, originCol, orientation, size, isPreview = false, isSunk = false }) => {
    const spriteWidth = size * CELL_SIZE + (size - 1) * GAP_SIZE;
    const spriteHeight = CELL_SIZE;

    const top = originRow * (CELL_SIZE + GAP_SIZE);
    const left = originCol * (CELL_SIZE + GAP_SIZE);

    const isVertical = orientation === 'VERTICAL';

    const containerStyle = {
      position: 'absolute',
      top,
      left,
      width: spriteWidth,
      height: spriteHeight,
      transformOrigin: 'top left',
      transform: isVertical ? 'rotate(90deg) translate(0, -100%)' : 'none',
      zIndex: isPreview ? 1 : 2,
      opacity: isPreview ? 0.6 : 1,
      pointerEvents: isPreview ? 'none' : 'auto',
    };

    const imgStyle = {
      width: spriteWidth,
      height: spriteHeight,
      maxWidth: 'none',
      maxHeight: 'none',
      display: 'block',
      filter: isSunk ? 'saturate(0) brightness(0.4)' : 'none',
    };

    return (
      <div style={containerStyle}>
        <img
          src={SHIP_SPRITES[shipType]}
          alt={shipType}
          style={imgStyle}
          draggable={false}
        />
        {isSunk && (
          <div
            style={{
              position: 'absolute',
              inset: 0,
              backgroundColor: 'rgba(153, 27, 27, 0.5)',
              borderRadius: 2,
            }}
          />
        )}
      </div>
    );
  }
);

ShipSprite.displayName = 'ShipSprite';

export { ShipSprite };
