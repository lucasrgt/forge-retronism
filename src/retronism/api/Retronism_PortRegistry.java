package retronism.api;

import java.util.HashMap;
import net.minecraft.src.TileEntity;

public class Retronism_PortRegistry {

    // Port info: {controllerX, controllerY, controllerZ, portType, portMode, originalBlockId}
    public static final int PORT_TYPE_ENERGY = 1;
    public static final int PORT_TYPE_FLUID = 2;
    public static final int PORT_TYPE_GAS = 3;

    public static final int PORT_MODE_INPUT = 1;
    public static final int PORT_MODE_OUTPUT = 2;

    private static HashMap ports = new HashMap();

    private static String key(int x, int y, int z) {
        return x + "," + y + "," + z;
    }

    public static void registerPort(int wx, int wy, int wz, int ctrlX, int ctrlY, int ctrlZ, int portType, int portMode) {
        registerPort(wx, wy, wz, ctrlX, ctrlY, ctrlZ, portType, portMode, 42); // default: iron block
    }

    public static void registerPort(int wx, int wy, int wz, int ctrlX, int ctrlY, int ctrlZ, int portType, int portMode, int originalBlockId) {
        ports.put(key(wx, wy, wz), new int[]{ctrlX, ctrlY, ctrlZ, portType, portMode, originalBlockId});
    }

    public static void unregisterPort(int wx, int wy, int wz) {
        ports.remove(key(wx, wy, wz));
    }

    public static int[] getPort(int wx, int wy, int wz) {
        return (int[]) ports.get(key(wx, wy, wz));
    }

    public static boolean isPort(int wx, int wy, int wz) {
        return ports.containsKey(key(wx, wy, wz));
    }

    public static boolean isPortOfType(int wx, int wy, int wz, int portType) {
        int[] info = getPort(wx, wy, wz);
        return info != null && info[3] == portType;
    }

    public static int getPortType(int wx, int wy, int wz) {
        int[] info = getPort(wx, wy, wz);
        return info != null ? info[3] : 0;
    }

    public static int getPortMode(int wx, int wy, int wz) {
        int[] info = getPort(wx, wy, wz);
        return info != null ? info[4] : 0;
    }

    public static int getOriginalBlockId(int wx, int wy, int wz) {
        int[] info = getPort(wx, wy, wz);
        return info != null && info.length > 5 ? info[5] : 42; // default iron
    }

    public static int[] getControllerPos(int wx, int wy, int wz) {
        int[] info = getPort(wx, wy, wz);
        if (info == null) return null;
        return new int[]{info[0], info[1], info[2]};
    }

    /**
     * Gets the controller TileEntity for a port position, or null if not a port.
     */
    public static TileEntity getControllerAt(net.minecraft.src.World world, int wx, int wy, int wz) {
        int[] info = getPort(wx, wy, wz);
        if (info == null) return null;
        return world.getBlockTileEntity(info[0], info[1], info[2]);
    }

    /**
     * Resolves a handler at position: returns the TileEntity there,
     * or if it's a registered port, returns the controller TileEntity.
     */
    public static TileEntity resolveHandler(net.minecraft.src.World world, int wx, int wy, int wz) {
        TileEntity te = world.getBlockTileEntity(wx, wy, wz);
        if (te != null) return te;
        return getControllerAt(world, wx, wy, wz);
    }

    public static void unregisterAllForController(int ctrlX, int ctrlY, int ctrlZ) {
        java.util.Iterator it = ports.entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry entry = (java.util.Map.Entry) it.next();
            int[] info = (int[]) entry.getValue();
            if (info[0] == ctrlX && info[1] == ctrlY && info[2] == ctrlZ) {
                it.remove();
            }
        }
    }
}
