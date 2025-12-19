import { useSpring, animated } from '@react-spring/web';
import { useDrag } from '@use-gesture/react';
import type { Pet } from '../../types';
import { formatBreed, formatSize } from '../../utils';

interface SwipeCardProps {
  pet: Pet;
  onSwipe: (direction: 'pass' | 'like') => void;
  onTap: () => void;
  isTop: boolean;
}

export function SwipeCard({ pet, onSwipe, onTap, isTop }: SwipeCardProps) {
  const [{ x, rotate, scale }, api] = useSpring(() => ({
    x: 0,
    rotate: 0,
    scale: isTop ? 1 : 0.92,
  }));

  const bind = useDrag(
    ({ active, movement: [mx], direction: [xDir], velocity: [vx] }) => {
      if (!isTop) return;

      const trigger = vx > 0.2 || Math.abs(mx) > 100;

      if (!active && trigger) {
        const flyOutX = xDir < 0 ? -600 : 600;
        api.start({
          x: flyOutX,
          rotate: xDir * 25,
          config: { friction: 50, tension: 200 },
          onRest: () => onSwipe(xDir < 0 ? 'pass' : 'like'),
        });
      } else {
        api.start({
          x: active ? mx : 0,
          rotate: active ? mx / 25 : 0,
          scale: 1,
          immediate: (key) => active && key !== 'scale',
        });
      }
    },
    {
      axis: 'x',
      filterTaps: true,
      enabled: isTop,
    }
  );

  const handleClick = () => {
    if (isTop) onTap();
  };

  const passOpacity = x.to((val) => Math.min(Math.abs(Math.min(val, 0)) / 100, 1));
  const likeOpacity = x.to((val) => Math.min(Math.max(val, 0) / 100, 1));

  const imageUrl = pet.imageUrls?.[0] || '/placeholder-pet.jpg';
  const ageDisplay = `${pet.age} ${pet.ageUnit === 'YEARS' ? (pet.age === 1 ? 'yr' : 'yrs') : (pet.age === 1 ? 'mo' : 'mos')}`;

  return (
    <animated.div
      {...bind()}
      onClick={handleClick}
      style={{
        x,
        rotate: rotate.to((r) => `${r}deg`),
        scale,
        touchAction: 'none',
        transformOrigin: 'center bottom',
      }}
      className={`absolute inset-0 rounded-3xl overflow-hidden shadow-2xl cursor-grab active:cursor-grabbing ${!isTop ? 'pointer-events-none' : ''}`}
    >
      {/* Pet image */}
      <img
        src={imageUrl}
        alt={pet.name}
        className="absolute inset-0 w-full h-full object-cover"
        draggable={false}
      />

      {/* Subtle vignette overlay */}
      <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-transparent to-black/20" />

      {/* Pass badge */}
      <animated.div
        style={{ opacity: passOpacity }}
        className="absolute top-6 left-6 px-5 py-2 border-[3px] border-red-500 rounded-xl bg-red-500/20 backdrop-blur-sm rotate-[-15deg]"
      >
        <span className="text-2xl font-black text-red-500 tracking-wide">NOPE</span>
      </animated.div>

      {/* Like badge */}
      <animated.div
        style={{ opacity: likeOpacity }}
        className="absolute top-6 right-6 px-5 py-2 border-[3px] border-emerald-400 rounded-xl bg-emerald-400/20 backdrop-blur-sm rotate-[15deg]"
      >
        <span className="text-2xl font-black text-emerald-400 tracking-wide">LIKE</span>
      </animated.div>

      {/* Pet info at bottom */}
      <div className="absolute inset-x-0 bottom-0 p-6">
        <div className="flex items-end justify-between">
          <div>
            <h2 className="text-3xl font-bold text-white mb-1 drop-shadow-lg">{pet.name}</h2>
            <p className="text-white/90 text-lg drop-shadow">
              {formatBreed(pet.breed)}
            </p>
            <div className="flex items-center gap-3 mt-2">
              <span className="px-3 py-1 bg-white/20 backdrop-blur-sm rounded-full text-sm text-white">
                {ageDisplay}
              </span>
              <span className="px-3 py-1 bg-white/20 backdrop-blur-sm rounded-full text-sm text-white">
                {formatSize(pet.size)}
              </span>
              <span className="px-3 py-1 bg-white/20 backdrop-blur-sm rounded-full text-sm text-white">
                {pet.sex === 'MALE' ? '♂' : '♀'}
              </span>
            </div>
          </div>
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              onTap();
            }}
            className="w-12 h-12 rounded-full bg-white/20 backdrop-blur-sm flex items-center justify-center text-white hover:bg-white/30 transition-colors"
            aria-label="View details"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </button>
        </div>
      </div>
    </animated.div>
  );
}
