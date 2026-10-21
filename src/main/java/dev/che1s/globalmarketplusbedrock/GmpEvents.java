package dev.che1s.globalmarketplusbedrock;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import studio.trc.bukkit.globalmarketplus.api.event.*;

/** Events invalidate display data; only result events trigger transaction navigation. */
final class GmpEvents implements Listener {
    private final FormManager manager;
    GmpEvents(FormManager manager) { this.manager = manager; }
    @EventHandler(priority = EventPriority.MONITOR)
    public void transaction(TransactionResultEvent e) { manager.transactionFinished(e.getTrader(), e.getMerchandise().getMerchandiseUID()); }
    @EventHandler(priority = EventPriority.MONITOR) public void auction(AuctionResultEvent e) { manager.invalidate(); }
    @EventHandler(priority = EventPriority.MONITOR) public void end(AuctionEndEvent e) { manager.invalidate(); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void bid(AuctionBidEvent e) { manager.invalidate(); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void sell(MerchandiseSellEvent e) { manager.invalidate(); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void buy(MerchandiseBuyEvent e) { manager.invalidate(); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void pickup(MailPickupEvent e) { manager.invalidate(); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void pickupAll(MailPickupAllEvent e) { manager.invalidate(); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void delete(MailDeleteEvent e) { manager.invalidate(); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void deleteAll(MailDeleteAllEvent e) { manager.invalidate(); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void send(MailSendEvent e) { manager.invalidate(); }
    @EventHandler(priority = EventPriority.MONITOR) public void loaded(MerchantDataLoadedEvent e) { manager.invalidate(); }
}
