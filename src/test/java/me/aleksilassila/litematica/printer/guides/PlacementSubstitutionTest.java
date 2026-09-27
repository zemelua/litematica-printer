package me.aleksilassila.litematica.printer.guides;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.stream.Stream;
import me.aleksilassila.litematica.printer.implementation.CropPlacement;
import me.aleksilassila.litematica.printer.implementation.EasyPlaceGrassSubstitution;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class PlacementSubstitutionTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // These identity/count tests do not load datapacks. 26.1 binds item components later.
        for (var item : List.of(Items.DIRT, Items.GRASS_BLOCK)) {
            var holder = item.builtInRegistryHolder();
            if (!holder.areComponentsBound())
                holder.bindComponents(net.minecraft.core.component.DataComponentMap.EMPTY);
        }
    }

    static Stream<Block> crops() {
        return Stream.of(Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS,
                Blocks.MELON_STEM, Blocks.PUMPKIN_STEM, Blocks.NETHER_WART,
                Blocks.COCOA, Blocks.SWEET_BERRY_BUSH);
    }

    @ParameterizedTest @MethodSource("crops")
    void allGrowthStagesCanBePlantedAndRecognizedAsComplete(Block crop) {
        var initial = crop.defaultBlockState();
        var age = (IntegerProperty) initial.getProperties().stream()
                .filter(p -> p.getName().equals("age")).findFirst().orElseThrow();
        for (int targetAge : age.getPossibleValues()) {
            BlockState target = initial.setValue(age, targetAge);
            assertSame(initial.setValue(age, 0), CropPlacement.placementTarget(target,
                    Blocks.AIR.defaultBlockState(), true));
            for (int actualAge : age.getPossibleValues()) {
                var actual = initial.setValue(age, actualAge);
                assertSame(actual, CropPlacement.placementTarget(target, actual, true));
                assertSame(target, CropPlacement.placementTarget(target, actual, false));
            }
        }
    }

    @Test void ageMatchingDoesNotAcceptDifferentPlantsOrCocoaFacing() {
        assertNotSame(Blocks.CARROTS.defaultBlockState(), CropPlacement.placementTarget(
                Blocks.WHEAT.defaultBlockState(), Blocks.CARROTS.defaultBlockState(), true));
        var cocoa = Blocks.COCOA.defaultBlockState();
        var rotated = cocoa.setValue(BlockStateProperties.HORIZONTAL_FACING,
                cocoa.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite());
        assertSame(cocoa, CropPlacement.placementTarget(cocoa, rotated, true));
        for (Block block : new Block[]{Blocks.OAK_SAPLING, Blocks.FIRE, Blocks.PITCHER_CROP,
                Blocks.ATTACHED_MELON_STEM, Blocks.STONE}) {
            var state = block.defaultBlockState();
            assertSame(state, CropPlacement.placementTarget(state, Blocks.AIR.defaultBlockState(), true));
        }
    }

    private BlockState grass(boolean enabled, ItemStack main, ItemStack off, boolean creative,
                             ItemStack... inventory) {
        return EasyPlaceGrassSubstitution.placementTarget(Blocks.GRASS_BLOCK.defaultBlockState(),
                enabled, main, off, creative, List.of(inventory));
    }

    @Test void heldDirtWorksEvenIfGrassIsAvailableAndInCreative() {
        var dirt = new ItemStack(Items.DIRT);
        var grass = new ItemStack(Items.GRASS_BLOCK);
        assertTrue(grass(true, dirt, grass, false, dirt, grass).is(Blocks.DIRT));
        assertTrue(grass(true, dirt, ItemStack.EMPTY, true).is(Blocks.DIRT));
        assertTrue(grass(true, ItemStack.EMPTY, dirt, false, grass).is(Blocks.DIRT));
        assertTrue(grass(true, grass, dirt, false).is(Blocks.GRASS_BLOCK));
    }

    @Test void inventoryFallbackPrefersGrassAndNeverInventsDirt() {
        var dirt = new ItemStack(Items.DIRT);
        var grass = new ItemStack(Items.GRASS_BLOCK);
        assertTrue(grass(true, ItemStack.EMPTY, ItemStack.EMPTY, false, dirt).is(Blocks.DIRT));
        assertTrue(grass(true, ItemStack.EMPTY, ItemStack.EMPTY, false, dirt, grass).is(Blocks.GRASS_BLOCK));
        assertTrue(grass(true, ItemStack.EMPTY, ItemStack.EMPTY, false).is(Blocks.GRASS_BLOCK));
        assertTrue(grass(true, ItemStack.EMPTY, ItemStack.EMPTY, true, dirt).is(Blocks.GRASS_BLOCK));
        assertTrue(grass(false, dirt, ItemStack.EMPTY, false, dirt).is(Blocks.GRASS_BLOCK));
    }

    @Test void dirtSubstitutionIsLimitedToGrassIncludingSnowyGrass() {
        var dirt = new ItemStack(Items.DIRT);
        for (Block block : new Block[]{Blocks.PODZOL, Blocks.MYCELIUM, Blocks.FARMLAND, Blocks.DIRT_PATH,
                Blocks.WHEAT, Blocks.STONE, Blocks.AIR}) {
            var state = block.defaultBlockState();
            assertSame(state, EasyPlaceGrassSubstitution.placementTarget(state, true,
                    dirt, ItemStack.EMPTY, false, List.of(dirt)));
        }
        var snowy = Blocks.GRASS_BLOCK.defaultBlockState().setValue(BlockStateProperties.SNOWY, true);
        assertTrue(EasyPlaceGrassSubstitution.placementTarget(snowy, true,
                dirt, ItemStack.EMPTY, false, List.of(dirt)).is(Blocks.DIRT));
        assertTrue(snowy.getValue(BlockStateProperties.SNOWY));
    }

    @Test void redirectsMatchExactlyOneSchematicStateReadInEachInstalledLitematicaMethod() throws Exception {
        var counts = new java.util.HashMap<String, Integer>();
        counts.put("doEasyPlaceAction", 0);
        counts.put("placementRestrictionInEffect", 0);
        try (var input = getClass().getClassLoader().getResourceAsStream("fi/dy/masa/litematica/util/WorldUtils.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                            String signature, String[] exceptions) {
                    if (!counts.containsKey(name)) return null;
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override public void visitMethodInsn(int opcode, String owner, String method,
                                                             String desc, boolean isInterface) {
                            if (owner.equals("net/minecraft/world/level/Level") && method.equals("getBlockState")
                                    && desc.equals("(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
                                counts.compute(name, (key, count) -> count + 1);
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        assertEquals(1, counts.get("doEasyPlaceAction"));
        assertEquals(1, counts.get("placementRestrictionInEffect"));
    }
}
