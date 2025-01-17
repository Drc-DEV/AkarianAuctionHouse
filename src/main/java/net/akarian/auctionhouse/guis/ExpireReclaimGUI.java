package net.akarian.auctionhouse.guis;

import lombok.Getter;
import lombok.Setter;
import net.akarian.auctionhouse.AuctionHouse;
import net.akarian.auctionhouse.listings.Listing;
import net.akarian.auctionhouse.utils.AkarianInventory;
import net.akarian.auctionhouse.utils.Chat;
import net.akarian.auctionhouse.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExpireReclaimGUI implements AkarianInventory {

    private final Chat chat = AuctionHouse.getInstance().getChat();
    private final Player player;
    private final AuctionHouseGUI auctionHouseGUI;
    private final int page;
    @Getter
    @Setter
    private List<Listing> listings;
    @Getter
    private Inventory inv;
    @Getter
    private int viewable;

    /**
     * Reclaim expired listings
     *
     * @param player          Player reclaiming listings
     * @param auctionHouseGUI Instance of AuctionH
     *                        ouseGUI
     * @param page            Page number
     */
    public ExpireReclaimGUI(Player player, AuctionHouseGUI auctionHouseGUI, int page) {
        this.player = player;
        this.auctionHouseGUI = auctionHouseGUI;
        this.listings = new ArrayList<>();
        this.page = page;
    }

    @Override
    public void onGUIClick(Inventory inv, Player p, int slot, ItemStack item, ClickType type) {
        switch (slot) {
            case 8:
                player.openInventory(auctionHouseGUI.getInventory());
                break;
            case 45:
                if (item.getType() == Material.NETHER_STAR)
                    player.openInventory(new ExpireReclaimGUI(player, auctionHouseGUI, (page - 1)).getInventory());
                return;
            case 53:
                if (item.getType() == Material.NETHER_STAR)
                    player.openInventory(new ExpireReclaimGUI(player, auctionHouseGUI, (page + 1)).getInventory());
                return;
        }
        if (slot == 8) {
            player.openInventory(auctionHouseGUI.getInventory());
        }

        //Is an Expired Listing
        if (slot >= 9 && slot <= 45) {
            Listing listing = AuctionHouse.getInstance().getListingManager().get(item);
            if (listing == null) return;
            switch (AuctionHouse.getInstance().getListingManager().reclaimExpire(listing, player, true)) {
                case -2:
                    chat.sendMessage(player, "&cThat listing is already reclaimed!");
                    break;
                case -1:
                    chat.sendMessage(player, "&cYou cannot hold that item.");
                    break;
            }


        }

    }

    @Override
    public Inventory getInventory() {
        inv = Bukkit.createInventory(this, 54, chat.format(AuctionHouse.getInstance().getMessages().getGui_er_title()));

        //Top Lining
        for (int i = 0; i <= 7; i++) {
            inv.setItem(i, ItemBuilder.build(AuctionHouse.getInstance().getConfigFile().getSpacerItem(), 1, " ", Collections.EMPTY_LIST));
        }

        //Return Button
        inv.setItem(8, ItemBuilder.build(Material.BARRIER, 1, AuctionHouse.getInstance().getMessages().getGui_buttons_rt(), AuctionHouse.getInstance().getMessages().getGui_buttons_rd()));

        //Bottom Lining
        for (int i = 45; i <= 53; i++) {
            inv.setItem(i, ItemBuilder.build(AuctionHouse.getInstance().getConfigFile().getSpacerItem(), 1, " ", Collections.EMPTY_LIST));
        }

        //Expired Listings
        updateInventory();

        //Previous Page
        if (page != 1) {
            ItemStack previous = ItemBuilder.build(Material.NETHER_STAR, 1, AuctionHouse.getInstance().getMessages().getGui_buttons_ppn(), AuctionHouse.getInstance().getMessages().getGui_buttons_ppd());
            inv.setItem(45, previous);
        }

        //Next Page
        if (listings.size() > 36 * page) {
            ItemStack next = ItemBuilder.build(Material.NETHER_STAR, 1, AuctionHouse.getInstance().getMessages().getGui_buttons_npn(), AuctionHouse.getInstance().getMessages().getGui_buttons_npd());
            inv.setItem(53, next);
        }


        return inv;
    }

    public void updateInventory() {
        listings.clear();
        listings.addAll(AuctionHouse.getInstance().getListingManager().getUnclaimedExpired(player.getUniqueId()));
        viewable = 0;

        int end = page * 36;
        int start = end - 36;
        int t = start;
        int slot = 9;

        for (int i = 9; i <= 44; i++) {
            inv.setItem(i, null);
        }

        for (int i = start; i <= end; i++) {
            if (listings.size() == t || t >= end) {
                break;
            }
            Listing listing = listings.get(i);
            inv.setItem(slot, listing.createExpiredListing(player));
            viewable++;
            slot++;
            t++;
        }

        //Previous Page
        if (page != 1) {
            ItemStack previous = ItemBuilder.build(Material.NETHER_STAR, 1, AuctionHouse.getInstance().getMessages().getGui_buttons_ppn(), AuctionHouse.getInstance().getMessages().getGui_buttons_ppd());
            inv.setItem(45, previous);
        } else {
            inv.setItem(45, ItemBuilder.build(AuctionHouse.getInstance().getConfigFile().getSpacerItem(), 1, " ", Collections.EMPTY_LIST));
        }

        //Next Page
        if (listings.size() > 36 * page) {
            ItemStack next = ItemBuilder.build(Material.NETHER_STAR, 1, AuctionHouse.getInstance().getMessages().getGui_buttons_npn(), AuctionHouse.getInstance().getMessages().getGui_buttons_npd());
            inv.setItem(53, next);
        } else {
            inv.setItem(53, ItemBuilder.build(AuctionHouse.getInstance().getConfigFile().getSpacerItem(), 1, " ", Collections.EMPTY_LIST));
        }

    }

}
