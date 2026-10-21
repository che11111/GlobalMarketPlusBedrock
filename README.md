 <p align="center">
  <img src="img/GlobalMarketPlusBedrock.png" alt="GlobalMarketPlusBedrock Logo" width="128">
</p>

<h1 align="center">GlobalMarketPlusBedrock</h1>
<p align="center">Bedrock Edition forms for GlobalMarketPlus</p>
<p align="center">
  <a href="#requirements-versions-and-server-software"><img src="https://img.shields.io/badge/Minecraft-1.16%2B-3fb950" alt="Minecraft 1.16+; see requirements below"></a>
  <a href="#requirements-versions-and-server-software"><img src="https://img.shields.io/badge/GlobalMarketPlus-1.4.3_API-5865F2" alt="Built against the GlobalMarketPlus 1.4.3 API"></a>
  <a href="#requirements-versions-and-server-software"><img src="https://img.shields.io/badge/floodgate-2.x-00AAFF" alt="Floodgate 2.x is required on the backend"></a>
  <a href="#requirements-versions-and-server-software"><img src="https://img.shields.io/badge/Java-8%2B_bytecode-orange" alt="Java 8 bytecode; runtime Java version follows the server"></a>
  <a href="#requirements-versions-and-server-software"><img src="https://img.shields.io/badge/Spigot%20%2F%20Paper-supported-blue" alt="Spigot and Paper supported"></a>
  <a href="#requirements-versions-and-server-software"><img src="https://img.shields.io/badge/Folia-conditional-blueviolet" alt="Conditional Folia support"></a>
</p>

<p align="center"><b>English</b> · <a href="README.zh-CN.md">简体中文</a></p>
<p align="center">
  <a href="https://github.com/che11111/GlobalMarketPlusBedrock/releases">Download</a> ·
  <a href="https://github.com/che11111/GlobalMarketPlusBedrock/issues">Report an Issue</a> ·
  <a href="https://bstats.org/plugin/bukkit/GlobalMarketPlusBedrock/34829">bStats</a>
</p>

GlobalMarketPlusBedrock is a Bukkit addon for [GlobalMarketPlus](https://www.spigotmc.org/resources/103648/). It provides native Bedrock Edition form interfaces for marketplace listings, auctions, shops, player profiles, and mailboxes to players connecting through Geyser and Floodgate.

Permissions, inventory operations, payments, taxes, and item delivery are still handled by GMP. This README also serves as the installation, configuration, and administration guide.

## Demo

<p align="center">
  <img src="img/202610294010951.gif" alt="GlobalMarketPlusBedrock demo" width="800">
</p>

## Features

- Provides Bedrock forms for selling, buying, auctions, merchandise management, mailboxes, shops, merchant profiles, and categories.
- Supports product search, filtering, pagination, sorting, and item icons. Shulker box and container contents can be previewed in read-only mode without allowing items to be moved.
- Supports listing creation and management, price and option changes, bidding and early auction purchases, and mailbox operations. Validation and transaction processing remain under GMP's control.


## Requirements, Versions, and Server Software

| Component | Requirements and Compatibility |
| --- | --- |
| Server software | **Minecraft 1.16+**; Spigot, Paper, Purpur, Folia, and others |
| GlobalMarketPlus | **Required**. Developed against **version 1.4.3.0**; other versions require separate verification |
| Floodgate | **Required**. Must be installed on the **backend server running this addon**. Installing it only on the proxy does not replace the backend API |
| Geyser | Required for Bedrock players to connect. Can be deployed on the proxy or backend |
| PlaceholderAPI | **Required** for parsing placeholders in the addon's messages |

The actual compatible version range depends on compatibility between GMP, Geyser, Floodgate, and PlaceholderAPI.

## Installation and Updates

1. Install GlobalMarketPlus, Floodgate, and PlaceholderAPI on the backend server, and configure Geyser to accept Bedrock connections.
2. Download the addon and place it in the `plugins/` directory.
3. Start the server, join using a Bedrock Edition client, and run `/gmpbe`, `/bemarket`, or the full command `/globalmarketplusbedrock`.

When upgrading, keep your existing configuration and language files. Reloading fills in missing default entries while preserving existing values. If a YAML file is invalid, the addon keeps the original file and falls back to the last working text or built-in messages.

After changing the configuration or language files, run `/gmpbe reload`.

## Commands and Permissions

| Command | Permission | Default |
| --- | --- | --- |
| `/globalmarketplusbedrock` | `globalmarketplusbedrock.use` | Everyone |
| `/gmpbe reload` | `globalmarketplusbedrock.admin` | OP |
| `/gmpbe update` | `globalmarketplusbedrock.admin` | OP |

Aliases: `/gmpbe`, `/bemarket`.

Full command: `/globalmarketplusbedrock`.

The permissions provided by this addon do **not** grant GMP permissions. Menu access follows GMP's configurable permission nodes, default permissions, permitted user groups, currency permissions, module toggles, and disabled-world settings. All write operations are still validated by GMP.

## Menu Feature Comparison

| GMP Built-in Menu | Bedrock Form Implementation |
| --- | --- |
| GlobalMarketGUI | Sell, buy, and auction listings; pagination, categories, GMP sorting, and manual refresh |
| TransactionGUI | Whole-lot or per-item purchases, integer quantity input, total quote calculation, and confirmation |
| AuctionGUI | View current bids and the highest bidder; enter bid amounts manually or adjust them by ±10/100/1000; buy out auctions early from merchandise management |
| MerchandiseGUI | My merchandise, delisting, whole-lot/per-item prices, never-expiring listings, unlimited merchandise, and toggles for hidden listings/seller names; manage other players' merchandise when authorized by UID |
| MailboxGUI | Filter pending, all, expired, and claimed mail; claim/delete individual messages; claim/delete the entire mailbox; categories and sorting |
| MerchantProfileGUI | Edit shop name and description; hide the shop, close it for business, lock the mailbox, configure sorting/categories, and refresh merchant caches |
| MerchantStoreGUI | Shop summary, description, statistics, paginated product browsing, and transactions |
| MerchantStoreListGUI | Shop list, pagination, and shop access; closed shops are hidden from visitor lists |
| CategoryGUI | Select or reset GMP categories and apply them to marketplace, shop, and mailbox lists |
| SearchGUI | Filter by material, seller, item name/description, price range, currency, category, and item type; find similar listings |
| SellInventoryGUI | Enter a price in a form, then use **GMP's native chest GUI** to complete equivalent-price or average-price bulk selling |
| BuyInventoryGUI | Enter a price in a form, then use **GMP's native chest GUI** to complete equivalent-price or average-price bulk buying |

The first ten menu groups have form-based workflows. The last two retain GMP's native bulk-operation interface.

A complete API for validating bulk listings, applying taxes, removing items, and returning items after failed transactions could not be found in GMP. Therefore, pure form-based bulk listing across multiple inventory slots has not been implemented.

### Transactions, Auctions, and Listing Creation

- Select a listing, choose a whole-lot or per-item purchase, and confirm the quantity and total quote. If the price, currency, transaction mode, or whole-lot quantity changes, confirmation is required again. Final taxes and settlement follow GMP's configuration.
- When a transaction result event arrives, the addon returns the player to the original list if they are still viewing the waiting form for that transaction. Other operations report results through chat and can be verified using the refresh or back buttons. Successfully dispatching a command is **not** treated as proof that a transaction succeeded.
- To create a listing, select selling, buying, or auction mode. Hold the target item during the countdown, then enter the price and quantity. Selling and buying require an explicit choice between whole-lot and per-item pricing. Before submission, the addon checks the held item and hotbar slot again.
- **A whole-lot price is the total price for the entire listing; a per-item price is the price of one item.** Entering `-1` in advanced settings disables the corresponding transaction mode. `0` is a valid numeric value; whether it is allowed depends on GMP's price limits. To cancel, close the form or click Cancel.
- Early auction purchases follow GMP's current highest-bid and transaction rules.

### Search, Shops, and Merchant Profiles

Search matches text content without regard to letter case and does not interpret regular expressions. Currency identifiers are matched exactly after case normalization.

Price filtering prioritizes whole-lot prices. If whole-lot trading is unavailable, the per-item price is used instead. The player's currently selected category is also applied to search results. If no listings are found, try resetting the category.

Open a shop from **All Shops** to view its summary, then browse its merchandise. Merchant profiles support renaming a shop and appending, editing, or deleting description lines across multiple pages.

Category names, sorting descriptions, localized item names, and GMP chat feedback come from GMP and use **GMP's own language and configuration**. Switching this addon's language between English and Chinese does not automatically translate GMP-provided text.

### Mailbox and Container Previews

**Claim All** and **Delete All** apply to the **entire target mailbox**, including messages hidden by the current category or status filter.

Deletion requires a second confirmation. It also deletes unclaimed items and cannot be undone. Claim permissions and inventory capacity are validated by GMP.

Shulker boxes and containers found in marketplace listings, managed merchandise, and mail can be displayed in **read-only content previews**. Depending on the configuration, previews can show original slot positions, quantities, names, descriptions, enchantments, and nested container contents.

Previews use cloned items and do not provide an inventory interface that allows items to be moved. Custom items must actually contain container metadata supported by Bukkit for their contents to be readable.

## Configuration

The configuration file is located at `plugins/GlobalMarketPlusBedrock/config.yml` and is generated automatically on the first startup.

After making changes, run `/gmpbe reload`. Language files are located at `plugins/GlobalMarketPlusBedrock/lang/zh_CN.yml` and `plugins/GlobalMarketPlusBedrock/lang/en_US.yml`.

```yaml
# Interface language: zh_CN or en_US
language: en_US

# Check GitHub for the latest stable release 5 seconds after startup and every 6 hours.
# This only displays a notification; it does not automatically replace the JAR.
check-for-updates: true

# bStats setting for this addon; also respects the global setting in plugins/bStats/config.yml.
bstats:
  enabled: true

# Automatically open the marketplace form when a player joins.
auto-open-on-join: false
# Delay before automatically opening the form, in seconds (1–3600).
auto-open-delay: 1

# Maximum number of entries per form-list page (1–100).
page-size: 20
# Product display cache duration in milliseconds (0–5000).
# 0 disables reuse of display snapshots; transactions still re-read GMP data.
display-cache-millis: 1000
# Convert supported player GMP menu commands into Bedrock forms.
# Write operations are still handled by GMP.
bridge-gmp-commands: true
# Maximum nesting depth for shulker box/container previews (1–8).
container-preview-depth: 4

# Item icons for products, mailbox items, and container contents.
item-icons:
  enabled: true
  auto:
    enabled: true
    # Built-in texture paths are based on Minecraft Java 26.3.
    # Changing the version alone does not regenerate the path mappings.
    version: '26.3'
    # Upgrade legacy built-in default URLs to the current version.
    # When disabled, existing older versions can still be retained.
    upgrade-legacy-default: true
    # {path} is an item/... or block/... path from the mapping.
    # Bedrock clients download the PNG directly.
    url-format: 'https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/{version}/assets/minecraft/textures/{path}.png'
  # Used when neither automatic nor manual mappings match.
  # Leave empty to display text only.
  fallback: ''
  # Override vanilla icons by Bukkit Material name.
  materials: {}
  # Override icons by Material + integer CustomModelData.
  custom-model-data: {}

# Display styles for function/navigation buttons and product buttons.
button-style:
  images: true
  symbols: true
  function-bold: true
  function-color: '&8'
  product-color: '&6'
  # Override function button icons/text symbols by role, e.g. search, back.
  functions: {}

# Countdown duration for holding the target item after clicking Create Listing (0–300 seconds).
countdown:
  seconds: 10
```

### Product Icon Configuration

`item-icons` only affects icons displayed on product, mailbox, and container-item buttons. It does not modify the items themselves or create/install a resource pack.

The priority order is **CustomModelData → Material override → built-in vanilla mapping → fallback → text only**.

Material mappings apply to all listings using that material. `custom-model-data` allows different thumbnail images for different integer custom model data values of the same base material. Set a value to `none` to explicitly disable the corresponding icon.

- `path;;textures/items/...` or `path;;textures/blocks/...`: Load a path from a resource pack currently loaded by the Bedrock client.
- `url;;https://...png`: Use a public image URL accessed directly by the Bedrock client. A URL being accessible from the server does not guarantee that every player's client can access it.
- `materials`: Map Bukkit Material names to icons, for example `DIAMOND`.
- `custom-model-data`: Define nested mappings by integer CMD under each Material name, for example CMD `1001` under `PAPER`.

```yaml
item-icons:
  materials:
    OMINOUS_BOTTLE: 'url;;https://example.com/icons/ominous_bottle.png'
    DIAMOND: 'path;;textures/items/diamond'
  custom-model-data:
    PAPER:
      '1001': 'url;;https://example.com/icons/merchant-token.png'
  fallback: 'path;;textures/items/book_normal'
```

Form icons are two-dimensional images and cannot directly display Java custom 3D models. For custom models, provide a separate PNG thumbnail or a texture from a Bedrock resource pack, then map it using Material/CMD.

If an icon URL is inaccessible, the client may display a blank image. The addon cannot determine whether an image was successfully loaded by the client.

### Updating Minecraft Icon Mappings

`tools/generate_item_textures.py` is a **development/build-time utility**, not a server plugin feature.

It reads a specified vanilla Java `client.jar`, parses texture references from `assets/minecraft/items/` and model parents, and writes only the `Material → item/... or block/...` path mappings to `src/main/resources/item-textures.yml`.

It does not copy PNG files, modify a running server, or automatically download updates. The script supports legacy model formats, newer item definitions, and sprite objects introduced in Minecraft 26.x.

The current repository mappings were generated from Minecraft Java 26.3. After obtaining the client JAR for another release, run the following command from the project root:

```bash
python3 tools/generate_item_textures.py /path/to/client.jar 26.3
```

Replace `26.3` with the client version. The new mappings will only be included after rebuilding the addon.

`item-icons.auto.version` controls the texture repository version used in default image URLs. **Changing this value alone does not update the mappings.**

The bundled 26.3 mapping currently contains 1,660 material-name/texture-path entries, including aliases for legacy server material names. It supports existing materials on compatible older servers and also covers item names introduced in 26.3.

This does **not** mean the addon is compatible with Minecraft 26.3 servers. The verified server API compatibility range is still described above.

Function button images use vanilla Bedrock resource paths configured through `button-style` and are independent of the product icon mappings described here.

## Frequently Asked Questions

| Issue | What to Check |
| --- | --- |
| The addon says only Bedrock players can use it, or no form appears | Check whether Floodgate identifies the player on the backend, whether proxy keys/configuration are correct, and whether the addon permissions are configured properly |
| The listing is empty | Check the selected category, search filters, hidden flags, currency permissions, and whether the shop is closed |
| An operation does not complete after submission | Check GMP chat feedback for permission, balance, tax, inventory, or cooldown restrictions, then refresh the list |
