CitiTraders Beta 2.0.3 README
Bukkit 1.4.7 R1 and Citizens 2.0.7 alpha

In this release 
===============
Add auto checking of  dev.bukkit.org for new releases of CitiTraders2
Updated admins wallet to prevent the storage of user sold items.
Allow admins to use /trader commands on npc they do not own.
Fixed /Trader fire from continuing firing traders after the first one.

New Permission nodes:
traders.admin.*
traders.admin.overRideOwner
traders.admin.loginVersionCheck

New Configuration value:
Check-For-Updates
When true allows the CitiTraders2 to check dev.bukkit.org for current release.


Release 2.0.2
===============
Fixed compatibility with bukkit 1.4.7 R1 and Citizens 2.0.7 alpha


Release 2.0.1:
=============
Gave ops the ability to modified traders that they do not own (will make permission node later)
Reworked help commands and error messages.



Disclaimer:
===========
This code is beta code, in essence if it causes your server to melt etc it is not my fault, you chose to run it.

Overview:
=========
CitiTraders provides a trading character type for Citizens 2.
This character uses vault to sell items in it's stockroom to other players.

Features:
=========
- Simple, ingame UI using inventories.
- Support for all items (inc. Enchanted items)
- (Future: Will support Village trader screen)
- Selling to NPCS
- Transaction safe*

(*: Transactions may bug and result in duped money due to unforseen bugs, if such a bug is found please report it to me)

Installation
============
YOU MUST HAVE CITIZENS 2 INSTALLED
YOU MUST HAVE VAULT AND AN ECONOMY PLUGIN INSTALLED
Drop CitiTraders into the plugins folder.

Configuration
=============
Valid trader npc types are set in config.yml

The profiles section allows you setup multiple tiers for traders (limits, eventually types and costs)

permission node traders.profile.profilename grants a user a profile.
default is assigned automatically, set it to 0 to prevent anyone w/o a trader profile perm creating trader npcs.

 

Creation
========

To create a trader, use the following command.
Using Citizens:
  /npc create npcname --char trader ...Other options...
using CitiTraders (allows prices  
/trader create name  
[TYPE] is an npc type set in the config.yml

Stocking a trader
=================
Right click a trader you own while holding a book, this will open up the stock room, place items you wish to sell in here.

Price lists
===========
use /trader sellprice amount
or  /trader buyprice amount
Then right click an NPC while holding the item you wish to set a price for.

sellprice sets the price an item is sold for
buyprice sets the price the npc will buy from players
NOTE: DAMAGE VALUES AND ENCHANTMENTS ARE RESPECTED, 


Wallets and you
===============
A wallet is a traders mechanism for paying for items.
wallet types:
- private :: a special economy independent wallet that. 
- owner :: uses the owners bank account directly.
- bank :: Uses a bank account his owner owns. Account name must be supplied
- admin :: A transdimensional wormhole linked to Scrooge McDuck's money bin.

/trader setwallet [type] [account name]

With a private wallet (default type), funds can be transfered using
/trader wallet give [amount]
/trader wallet take [amount]
Trading with an npc
===================
1) Right click the npc
2) in the inventory left clicking an item will display prices in chat, 
   Shift clicking will move through the screens and purchase items.

3) The first screen displays all items in stock that have a price set for sale. 
   Shift clicking will select an item. Left click price of one item.

4) The second screen displays the selected item in various quantities (64,32,16,8,4,2,1). 
   Shift click purchases that stack, left click shows price of that stack.

5) Close the inventory to finish trading.

Selling to an npc
=================
1) Left click the NPC

2) Shift click will tell you price per item and for that stack

3) Drag into the top inventory to sell.

4) Close to complete transaction

5) Any additional items will be dropped at your feet.

Firing an npc
=============
1) /trader fire
2) right click NPC
3) if npc's inventory is empty and he has no money in his pockets (private wallet only) he will be fired. 