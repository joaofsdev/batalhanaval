import { useState } from 'react';
import Modal from '../shared/Modal';

const SLIDES = [
  {
    icon: 'thunderstorm',
    title: 'MODO TEMPESTADE',
    content: (
      <div className="flex flex-col gap-3">
        <p className="text-sm text-on-surface-variant leading-relaxed">
          Neste modo, além dos tiros, você terá uma <strong className="text-on-surface">habilidade tática</strong> e enfrentará{' '}
          <strong className="text-on-surface">eventos climáticos</strong> aleatórios.
        </p>
        <div className="flex flex-col gap-2 border-l-2 border-primary pl-3">
          <p className="text-xs text-on-surface-variant">
            <span className="text-primary font-bold">HABILIDADE</span> — aparece no painel inferior. Use estrategicamente — ela{' '}
            <strong className="text-on-surface">rotaciona a cada 3-6 turnos</strong>.
          </p>
          <p className="text-xs text-on-surface-variant">
            <span className="text-warning font-bold">EVENTOS</span> — a cada 3-6 turnos, um evento climático será resolvido antes do próximo tiro.
          </p>
        </div>
      </div>
    ),
  },
  {
    icon: 'auto_awesome',
    title: 'HABILIDADES',
    content: (
      <div className="flex flex-col gap-3">
        <p className="text-xs text-on-surface-variant">
          Você recebe 1 habilidade aleatória. Pode usá-la <strong className="text-on-surface">1 vez</strong> antes de rotacionar:
        </p>
        <div className="grid grid-cols-1 gap-2">
          <div className="flex items-start gap-2 bg-surface-container-high p-2 border border-outline-variant">
            <span className="material-symbols-outlined text-primary text-base">radar</span>
            <div>
              <p className="text-xs font-bold text-on-surface">RADAR</p>
              <p className="text-[11px] text-on-surface-variant">Revela presença de navios numa área 3×3</p>
            </div>
          </div>
          <div className="flex items-start gap-2 bg-surface-container-high p-2 border border-outline-variant">
            <span className="material-symbols-outlined text-primary text-base">rocket_launch</span>
            <div>
              <p className="text-xs font-bold text-on-surface">TORPEDO DUPLO</p>
              <p className="text-[11px] text-on-surface-variant">Dispara 2 tiros adjacentes verticalmente</p>
            </div>
          </div>
          <div className="flex items-start gap-2 bg-surface-container-high p-2 border border-outline-variant">
            <span className="material-symbols-outlined text-primary text-base">shield</span>
            <div>
              <p className="text-xs font-bold text-on-surface">ESCUDO</p>
              <p className="text-[11px] text-on-surface-variant">Anula o próximo tiro que você receber</p>
            </div>
          </div>
          <div className="flex items-start gap-2 bg-surface-container-high p-2 border border-outline-variant">
            <span className="material-symbols-outlined text-primary text-base">destruction</span>
            <div>
              <p className="text-xs font-bold text-on-surface">BOMBARDEIO EM LINHA</p>
              <p className="text-[11px] text-on-surface-variant">Ataca toda uma linha ou coluna do tabuleiro</p>
            </div>
          </div>
        </div>
        <p className="text-[11px] text-warning border border-warning/30 bg-warning/5 px-2 py-1">
          ⚠️ Habilidades ficam bloqueadas durante turnos de tempestade.
        </p>
      </div>
    ),
  },
  {
    icon: 'cloudy_snowing',
    title: 'EVENTOS CLIMÁTICOS',
    content: (
      <div className="flex flex-col gap-3">
        <p className="text-xs text-on-surface-variant">
          A cada 3-6 turnos, um evento atinge o campo de batalha:
        </p>
        <div className="flex flex-col gap-2">
          <div className="flex items-center gap-2">
            <span className="text-base">🌫️</span>
            <div>
              <span className="text-xs font-bold text-on-surface">NEBLINA</span>
              <span className="text-[11px] text-on-surface-variant ml-1">— resultados dos tiros ficam ocultos até o turno seguinte</span>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-base">🌊</span>
            <div>
              <span className="text-xs font-bold text-on-surface">MARÉ ALTA</span>
              <span className="text-[11px] text-on-surface-variant ml-1">— uma linha fica bloqueada para disparos neste turno</span>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-base">💨</span>
            <div>
              <span className="text-xs font-bold text-on-surface">CORRENTE MARÍTIMA</span>
              <span className="text-[11px] text-on-surface-variant ml-1">— navios podem se deslocar 1 célula aleatoriamente</span>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-base">☀️</span>
            <div>
              <span className="text-xs font-bold text-on-surface">CALMARIA</span>
              <span className="text-[11px] text-on-surface-variant ml-1">— tiro bônus concedido ao jogador da vez</span>
            </div>
          </div>
        </div>
      </div>
    ),
  },
  {
    icon: 'warning',
    title: 'REGRAS IMPORTANTES',
    content: (
      <div className="flex flex-col gap-3">
        <div className="flex flex-col gap-2">
          <div className="flex items-start gap-2">
            <span className="material-symbols-outlined text-error text-base mt-0.5">timer</span>
            <p className="text-xs text-on-surface-variant">
              Você tem <strong className="text-on-surface">60 segundos</strong> para disparar. Se não disparar, seu turno é pulado.
            </p>
          </div>
          <div className="flex items-start gap-2">
            <span className="material-symbols-outlined text-error text-base mt-0.5">block</span>
            <p className="text-xs text-on-surface-variant">
              <strong className="text-error">3 turnos pulados</strong> seguidos = derrota automática (AFK).
            </p>
          </div>
          <div className="flex items-start gap-2">
            <span className="material-symbols-outlined text-warning text-base mt-0.5">sync</span>
            <p className="text-xs text-on-surface-variant">
              Habilidades <strong className="text-on-surface">rotacionam a cada 3-6 turnos</strong> — use antes de perder!
            </p>
          </div>
          <div className="flex items-start gap-2">
            <span className="material-symbols-outlined text-secondary text-base mt-0.5">bolt</span>
            <p className="text-xs text-on-surface-variant">
              O evento climático é resolvido junto com o primeiro tiro do turno de tempestade.
            </p>
          </div>
        </div>
      </div>
    ),
  },
];

const STORAGE_KEY = 'bn_storm_tutorial_seen';

const StormTutorial = ({ open, onClose }) => {
  const [currentSlide, setCurrentSlide] = useState(0);

  const isLast = currentSlide === SLIDES.length - 1;

  const handleNext = () => {
    if (isLast) {
      handleFinish();
    } else {
      setCurrentSlide((prev) => prev + 1);
    }
  };

  const handlePrev = () => {
    setCurrentSlide((prev) => Math.max(0, prev - 1));
  };

  const handleFinish = () => {
    localStorage.setItem(STORAGE_KEY, 'true');
    setCurrentSlide(0);
    onClose?.();
  };

  const handleSkip = () => {
    handleFinish();
  };

  const slide = SLIDES[currentSlide];

  return (
    <Modal open={open} onClose={handleSkip}>
      <div className="flex flex-col">
        <div className="flex items-center gap-3 px-6 pt-6 pb-4 border-b border-outline-variant">
          <span className="material-symbols-outlined text-primary text-2xl">{slide.icon}</span>
          <h2 className="font-headline-md text-headline-md text-primary tracking-widest">
            {slide.title}
          </h2>
        </div>

        <div className="px-6 py-5 min-h-[260px] flex flex-col justify-center">
          {slide.content}
        </div>

        <div className="flex items-center justify-between px-6 py-4 border-t border-outline-variant bg-surface-container-high/50">
          <div className="flex items-center gap-1.5">
            {SLIDES.map((_, i) => (
              <div
                key={i}
                className={`w-2 h-2 rounded-full transition-colors ${
                  i === currentSlide ? 'bg-primary' : 'bg-outline-variant'
                }`}
              />
            ))}
          </div>

          <div className="flex items-center gap-2">
            {!isLast && (
              <button
                onClick={handleSkip}
                className="px-3 py-1.5 text-on-surface-variant font-label-caps text-label-caps hover:text-on-surface transition-colors"
              >
                PULAR
              </button>
            )}
            {currentSlide > 0 && (
              <button
                onClick={handlePrev}
                className="px-3 py-1.5 border border-outline-variant text-on-surface-variant font-label-caps text-label-caps hover:border-primary hover:text-primary transition-colors"
              >
                VOLTAR
              </button>
            )}
            <button
              onClick={handleNext}
              className="px-4 py-1.5 bg-primary text-on-primary font-label-caps text-label-caps hover:bg-primary/90 transition-colors"
            >
              {isLast ? 'ENTENDI' : 'PRÓXIMO'}
            </button>
          </div>
        </div>
      </div>
    </Modal>
  );
};

export { STORAGE_KEY };
export default StormTutorial;
