package cn.charlotte.pit.util;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.config.PitConfig;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FaweUtils {

    /** Resolve/validate on the main thread; read and paste on the owning FAWE job. */
    public static File requireSchematic(String name) {
        File file = new File(ThePit.getInstance().getDataFolder(), "schem/" + name);
        if (!file.isFile()) throw new IllegalStateException("找不到事件建筑文件: " + file);
        if (ClipboardFormats.findByFile(file) == null) throw new IllegalStateException("无法识别事件建筑格式: " + file);
        return file;
    }

    public static void pasteSchematicOrThrow(EditSession session, BlockVector3 at, File file, boolean ignoreAir) throws Exception {
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) throw new IOException("Unknown schematic format: " + file);
        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            Clipboard clipboard = reader.read();
            Operations.complete(new ClipboardHolder(clipboard).createPaste(session).to(at).ignoreAirBlocks(ignoreAir).build());
        }
        // The owning batch closes this session and waits for its writes, propagating failures to the lifecycle.
    }

    public static void closeDoor(EditSession session) {
        PitConfig config = ThePit.getInstance().getPitConfig();
        Location location = config.getPitDoorCenter();
        BlockVector3 vector = BlockVector3.at(location.getX(), location.getY(), location.getZ());
        pasteSchematic(session, vector, "pit_door.schem", true);
    }

    public static void openDoor(EditSession session) {
        World world = Bukkit.getWorld("world");
        try (EditSession target = WorldEdit.getInstance().newEditSession(new BukkitWorld(world))) {
            if (session != null) {
                session.undo(target);   // ← 回滚所有修改
                target.flushQueue();
            }
            // try 块结束时 target 会自动 flush + close
        }
    }


    public static BlockVector3 toBlockVector3(Location location) {
        return BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static void pasteSchematic(EditSession session, BlockVector3 pasteLoc, String schem, boolean ignoreAir) {
        File file = new File(ThePit.getInstance().getDataFolder(), "schem/" + schem);
        if (!file.exists()) {
            Bukkit.getLogger().warning("找不到 " + schem);
            return;
        }

        ClipboardFormat fmt = ClipboardFormats.findByFile(file);
        try (ClipboardReader reader = fmt.getReader(new FileInputStream(file))) {              // 直接复用已保存的 session

            Clipboard cb = reader.read();
            Operations.complete(new ClipboardHolder(cb)
                    .createPaste(session)
                    .to(pasteLoc)
                    .ignoreAirBlocks(ignoreAir)
                    .build());
            session.flushQueue();
            // 这里不需要额外 flush，Session 会在 onInactive 里统一 flush/close
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
