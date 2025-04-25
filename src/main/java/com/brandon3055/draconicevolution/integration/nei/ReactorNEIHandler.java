package com.brandon3055.draconicevolution.integration.nei;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.ModItems;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.TileReactorCore;

import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;

public class ReactorNEIHandler extends TemplateRecipeHandler {

    @Override
    public String getGuiTexture() {
        return new ResourceLocation("draconicevolution", "textures/gui/nei/reactor.png").toString();
    }

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal("nei.recipe.reactor.category");
    }

    @Override
    public String getOverlayIdentifier() {
        return "draconicevolution.reactor";
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        if (outputId.equals("draconicevolution.reactor") && getClass() == ReactorNEIHandler.class) {
            final CachedReactorRecipe cachedRecipe = new CachedReactorRecipe();
            this.arecipes.add(cachedRecipe);
        } else {
            super.loadCraftingRecipes(outputId, results);
        }
    }

    @Override
    public void loadCraftingRecipes(final ItemStack result) {
        if (NEIServerUtils.areStacksSameTypeCrafting(new ItemStack(ModItems.chaosShard), result)) {
            final CachedReactorRecipe cachedRecipe = new CachedReactorRecipe();
            this.arecipes.add(cachedRecipe);
        }
    }

    @Override
    public void loadUsageRecipes(final ItemStack ingredient) {
        final CachedReactorRecipe cachedRecipe = new CachedReactorRecipe();
        if ((cachedRecipe.contains(cachedRecipe.ingredients, ingredient.getItem()))) {
            if (cachedRecipe.contains(cachedRecipe.ingredients, ingredient)) {
                cachedRecipe.setIngredientPermutation(cachedRecipe.ingredients, ingredient);
                this.arecipes.add(cachedRecipe);
            }
        }
    }

    @Override
    public void drawExtras(int recipe) {
        this.drawProgressBar(16, 18, 176, 10, 24, 28, 1200, 1);
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glTranslated(85, 32, 100);
        int scale = 260;
        GL11.glScaled(scale, scale, scale);
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GL11.glDisable(GL11.GL_CULL_FACE);

        TileEntityRendererDispatcher.instance.renderTileEntityAt(new TileReactorCore(), -0.5D, -0.5D, -0.5D, 0.0F);

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    @Override
    public void loadTransferRects() {
        transferRects.add(new RecipeTransferRect(new Rectangle(16, 18, 24, 28), "draconicevolution.reactor"));
    }

    private class CachedReactorRecipe extends CachedRecipe {

        private final List<PositionedStack> ingredients;
        private final PositionedStack result;

        public CachedReactorRecipe() {
            this.result = new PositionedStack(new ItemStack(ModItems.chaosShard), 17, 47);
            this.ingredients = new ArrayList<>();
            ItemStack[] inputList = { new ItemStack(ModBlocks.draconicBlock), new ItemStack(ModItems.draconicIngot),
                    new ItemStack(ModItems.nugget, 1, 1) };
            this.ingredients.add(new PositionedStack(inputList, 17, 4));
        }

        @Override
        public PositionedStack getResult() {
            return this.result;
        }

        @Override
        public List<PositionedStack> getIngredients() {
            return getCycledIngredients(ReactorNEIHandler.this.cycleticks / 20, this.ingredients);
        }
    }
}
