export const SHIP_FLEET = [
  { type: 'CARRIER', size: 5, label: 'Porta-aviões' },
  { type: 'BATTLESHIP', size: 4, label: 'Encouraçado' },
  { type: 'CRUISER', size: 3, label: 'Cruzador' },
  { type: 'SUBMARINE', size: 3, label: 'Submarino' },
  { type: 'DESTROYER', size: 2, label: 'Contratorpedeiro' },
];

export const ORIENTATIONS = { HORIZONTAL: 'HORIZONTAL', VERTICAL: 'VERTICAL' };

export const GAME_STATUS = {
  WAITING: 'WAITING',
  PLACING: 'PLACING',
  IN_PROGRESS: 'IN_PROGRESS',
  FINISHED: 'FINISHED',
};

export const SHOT_RESULT = { MISS: 'MISS', HIT: 'HIT', SUNK: 'SUNK' };
