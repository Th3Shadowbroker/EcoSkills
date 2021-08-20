package com.willfp.ecoskills.skills

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.integrations.placeholder.PlaceholderEntry
import com.willfp.eco.util.NumberUtils
import com.willfp.eco.util.StringUtils
import com.willfp.ecoskills.*
import com.willfp.ecoskills.config.SkillConfig
import com.willfp.ecoskills.effects.Effects
import com.willfp.ecoskills.stats.Stats
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import java.util.*

abstract class Skill(
    val id: String
) : Listener {
    protected val plugin: EcoPlugin = EcoSkillsPlugin.getInstance()

    val key: NamespacedKey = plugin.namespacedKeyFactory.create(id)
    val xpKey: NamespacedKey = plugin.namespacedKeyFactory.create(id + "_progress")
    val config: Config
    lateinit var name: String
    lateinit var description: String
    lateinit var gui: SkillGUI
    var maxLevel: Int = 50
    private val rewards: MutableMap<SkillObject, Int>

    init {
        config = SkillConfig(this.id, this.javaClass, plugin)
        rewards = HashMap()

        Skills.registerNewSkill(this)
    }

    fun update() {
        name = plugin.langYml.getString("skills.$id.name")
        description = plugin.langYml.getString("skills.$id.description")
        maxLevel = config.getInt("max-level")
        rewards.clear()
        for (string in config.getStrings("level-up-rewards")) {
            val split = string.split(":")
            val asEffect = Effects.getByID(split[0].lowercase())
            val asStat = Stats.getByID(split[0].lowercase())
            if (asEffect != null) {
                rewards[asEffect] = split[1].toInt()
            }
            if (asStat != null) {
                rewards[asStat] = split[1].toInt()
            }
        }

        PlaceholderEntry(
            id,
            { player -> player.getSkillLevel(this).toString() },
            true
        ).register()

        PlaceholderEntry(
            "${id}_numeral",
            { player -> NumberUtils.toNumeral(player.getSkillLevel(this)) },
            true
        ).register()

        postUpdate()

        gui = SkillGUI(plugin, this)
    }

    fun getLevelUpRewards(): Collection<SkillObject> {
        return rewards.keys
    }

    fun getLevelUpReward(skillObject: SkillObject): Int {
        return rewards[skillObject] ?: 0
    }

    fun getRewardsMessages(player: Player): MutableList<String> {
        val messages = ArrayList<String>()
        for (string in this.config.getStrings("rewards-messages", false)) {
            messages.add(StringUtils.format(string, player))
        }
        return messages
    }

    fun getGUIRewardsMessages(player: Player): MutableList<String> {
        val lore = ArrayList<String>()
        for (string in this.config.getStrings("rewards-gui-lore", false)) {
            lore.add(StringUtils.format(string, player))
        }
        return lore
    }

    fun getGUILore(player: Player): MutableList<String> {
        val lore = ArrayList<String>()
        for (string in this.config.getStrings("gui-lore", false)) {
            lore.add(StringUtils.format(string, player))
        }
        return lore
    }

    open fun postUpdate() {
        // Override when needed
    }

    fun getExpForLevel(level: Int): Int {
        return this.plugin.configYml.getInts("skills.level-xp-requirements")[level - 1] ?: Integer.MAX_VALUE
    }
}