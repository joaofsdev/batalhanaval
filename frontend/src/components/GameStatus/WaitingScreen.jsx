const WaitingScreen = () => (
  <div className="flex flex-col items-center justify-center gap-4 py-16">
    <div className="w-10 h-10 border-4 border-blue-400 border-t-transparent rounded-full animate-spin" />
    <p className="text-slate-300 text-lg">Aguardando oponente entrar na partida...</p>
  </div>
);

export default WaitingScreen;
