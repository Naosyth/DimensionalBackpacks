package com.naosyth.dimensionalbackpacks.dimensional_backpacks.client.compat

/** Assigns at most one available item to each recipe slot, rearranging earlier choices when needed. */
internal object RecipeAssignment {
    fun assign(capacities: List<Int>, candidates: List<List<Int>>): IntArray {
        val assigned = IntArray(candidates.size) { -1 }
        val owners = List(capacities.size) { mutableListOf<Int>() }
        fun place(slot: Int, seen: BooleanArray): Boolean {
            for (index in candidates[slot]) {
                if (seen[index] || capacities[index] <= 0) continue
                seen[index] = true
                if (owners[index].size < capacities[index]) {
                    owners[index].add(slot)
                    assigned[slot] = index
                    return true
                }
                for (other in owners[index].toList()) {
                    if (place(other, seen)) {
                        owners[index].remove(other)
                        owners[index].add(slot)
                        assigned[slot] = index
                        return true
                    }
                }
            }
            return false
        }
        for (slot in candidates.indices) place(slot, BooleanArray(capacities.size))
        return assigned
    }
}
