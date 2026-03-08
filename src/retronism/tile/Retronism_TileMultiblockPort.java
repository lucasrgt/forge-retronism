package retronism.tile;

import net.minecraft.src.*;
import retronism.api.Retronism_IEnergyReceiver;
import retronism.api.Retronism_IFluidHandler;
import retronism.api.Retronism_IGasHandler;

public class Retronism_TileMultiblockPort extends TileEntity
        implements Retronism_IEnergyReceiver, Retronism_IFluidHandler, Retronism_IGasHandler {

    public int controllerX, controllerY, controllerZ;
    public int portType; // 0=none, 1=energy, 2=fluid, 3=gas
    public int portMode; // 0=none, 1=input, 2=output

    public static final int TYPE_NONE = 0;
    public static final int TYPE_ENERGY = 1;
    public static final int TYPE_FLUID = 2;
    public static final int TYPE_GAS = 3;

    public static final int MODE_NONE = 0;
    public static final int MODE_INPUT = 1;
    public static final int MODE_OUTPUT = 2;

    private TileEntity getController() {
        if (worldObj == null) return null;
        return worldObj.getBlockTileEntity(controllerX, controllerY, controllerZ);
    }

    public void notifyControllerRemoved() {
        TileEntity ctrl = getController();
        if (ctrl != null && ctrl instanceof Retronism_TileOzonizer) {
            ((Retronism_TileOzonizer) ctrl).isFormed = false;
        }
    }

    // --- IEnergyReceiver ---
    @Override
    public int receiveEnergy(int amount) {
        if (portType != TYPE_ENERGY) return 0;
        TileEntity ctrl = getController();
        if (ctrl instanceof Retronism_IEnergyReceiver) {
            return ((Retronism_IEnergyReceiver) ctrl).receiveEnergy(amount);
        }
        return 0;
    }

    @Override
    public int getStoredEnergy() {
        TileEntity ctrl = getController();
        if (ctrl instanceof Retronism_IEnergyReceiver) {
            return ((Retronism_IEnergyReceiver) ctrl).getStoredEnergy();
        }
        return 0;
    }

    @Override
    public int getMaxEnergy() {
        TileEntity ctrl = getController();
        if (ctrl instanceof Retronism_IEnergyReceiver) {
            return ((Retronism_IEnergyReceiver) ctrl).getMaxEnergy();
        }
        return 0;
    }

    // --- IFluidHandler ---
    @Override
    public int receiveFluid(int type, int amountMB) {
        if (portType != TYPE_FLUID || portMode != MODE_INPUT) return 0;
        TileEntity ctrl = getController();
        if (ctrl instanceof Retronism_IFluidHandler) {
            return ((Retronism_IFluidHandler) ctrl).receiveFluid(type, amountMB);
        }
        return 0;
    }

    @Override
    public int extractFluid(int type, int amountMB) {
        if (portType != TYPE_FLUID || portMode != MODE_OUTPUT) return 0;
        TileEntity ctrl = getController();
        if (ctrl instanceof Retronism_IFluidHandler) {
            return ((Retronism_IFluidHandler) ctrl).extractFluid(type, amountMB);
        }
        return 0;
    }

    @Override public int getFluidType() {
        TileEntity ctrl = getController();
        return ctrl instanceof Retronism_IFluidHandler ? ((Retronism_IFluidHandler) ctrl).getFluidType() : 0;
    }
    @Override public int getFluidAmount() {
        TileEntity ctrl = getController();
        return ctrl instanceof Retronism_IFluidHandler ? ((Retronism_IFluidHandler) ctrl).getFluidAmount() : 0;
    }
    @Override public int getFluidCapacity() {
        TileEntity ctrl = getController();
        return ctrl instanceof Retronism_IFluidHandler ? ((Retronism_IFluidHandler) ctrl).getFluidCapacity() : 0;
    }

    // --- IGasHandler ---
    @Override
    public int receiveGas(int type, int amountMB) {
        if (portType != TYPE_GAS || portMode != MODE_INPUT) return 0;
        TileEntity ctrl = getController();
        if (ctrl instanceof Retronism_IGasHandler) {
            return ((Retronism_IGasHandler) ctrl).receiveGas(type, amountMB);
        }
        return 0;
    }

    @Override
    public int extractGas(int type, int amountMB) {
        if (portType != TYPE_GAS || portMode != MODE_OUTPUT) return 0;
        TileEntity ctrl = getController();
        if (ctrl instanceof Retronism_IGasHandler) {
            return ((Retronism_IGasHandler) ctrl).extractGas(type, amountMB);
        }
        return 0;
    }

    @Override public int getGasType() {
        TileEntity ctrl = getController();
        return ctrl instanceof Retronism_IGasHandler ? ((Retronism_IGasHandler) ctrl).getGasType() : 0;
    }
    @Override public int getGasAmount() {
        TileEntity ctrl = getController();
        return ctrl instanceof Retronism_IGasHandler ? ((Retronism_IGasHandler) ctrl).getGasAmount() : 0;
    }
    @Override public int getGasCapacity() {
        TileEntity ctrl = getController();
        return ctrl instanceof Retronism_IGasHandler ? ((Retronism_IGasHandler) ctrl).getGasCapacity() : 0;
    }

    // --- NBT ---
    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        controllerX = nbt.getInteger("CtrlX");
        controllerY = nbt.getInteger("CtrlY");
        controllerZ = nbt.getInteger("CtrlZ");
        portType = nbt.getInteger("PortType");
        portMode = nbt.getInteger("PortMode");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("CtrlX", controllerX);
        nbt.setInteger("CtrlY", controllerY);
        nbt.setInteger("CtrlZ", controllerZ);
        nbt.setInteger("PortType", portType);
        nbt.setInteger("PortMode", portMode);
    }
}
