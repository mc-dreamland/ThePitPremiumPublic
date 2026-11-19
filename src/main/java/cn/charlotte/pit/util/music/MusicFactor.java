//package cn.charlotte.pit.util.music;
//
//import cn.charlotte.pit.util.cooldown.Cooldown;
//import lombok.Getter;
//import org.bukkit.Bukkit;
//import org.bukkit.entity.Player;
//
//import java.util.ArrayDeque;
//import java.util.Queue;
//
//public class MusicFactor {
//    private Thread musicThread;
//    @Getter private Song song;
//    private short musicTime;
//    private short ticks;
//
//    @Override
//    public boolean equals(Object obj) {
//        return super.equals(obj);
//    }
//
//    private Cooldown cooldown;
//
//    public MusicFactor(){
//
//    }
//
//    public boolean toPlaying(Song song){
//        if(this.musicThread.isAlive()) return false;
//        this.song = song;
//        this.musicThread = new Thread(()->{
//               while (this.song.getLength() + this.musicTime < System.currentTimeMillis() ){
//                   synchronized(MusicFactor.this){
//                       ticks++;
//                       if(ticks <= this.song.getLength()) continue;
//                       Bukkit.getOnlinePlayers().forEach(p ->{playTick(p,ticks);});
//                   }
//               }
//        });
//        return true;
//    }
//
//    public void playTick(Player p, int tick) {
//        if (!p.getWorld().getName().equals(p.getWorld().getName())) {
//            // not in same world
//            return;
//        }
//        byte playerVolume = NoteBlockPlayerMain.getPlayerVolume(p);
//
//        for (Layer l : song.getLayerHashMap().values()) {
//            Note note = l.getNote(tick);
//            if (note == null) {
//                continue;
//            }
//            p.playSound(p,
//                    Instrument.getInstrument(note.instrument()),
//                    (l.getVolume() * (int) this.song. * (int) playerVolume) / 1000000f,
//                    NotePitch.getPitch(note.key() - 33));
//        }
//    }
//
//}
