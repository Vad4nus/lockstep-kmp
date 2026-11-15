package io.github.vad4nus.lockstep.skie

import co.touchlab.skie.phases.InitPhase
import co.touchlab.skie.phases.other.AddFoundationImportsPhase
import co.touchlab.skie.phases.other.LoadCustomSwiftSourceFilesPhase
import co.touchlab.skie.phases.sir.CommitSirIsReplacedPropertyPhase
import co.touchlab.skie.spi.SkiePluginRegistrar

class LockstepSkiePluginRegistrar : SkiePluginRegistrar {

    override fun register(initPhaseContext: InitPhase.Context) {
        val scheduler = initPhaseContext.skiePhaseScheduler
        val run = LockstepRun()

        scheduler.kirPhases.modify {
            add(LockstepKirPhase(run))
        }

        scheduler.sirPhases.modify {
            val commit = indexOf(CommitSirIsReplacedPropertyPhase)
            check(commit >= 0) { "lockstep: unsupported SKIE version, phase CommitSirIsReplacedPropertyPhase not found" }
            add(commit, LockstepRenamePhase(run))
            val load = indexOf(LoadCustomSwiftSourceFilesPhase)
            check(load >= 0) { "lockstep: unsupported SKIE version, phase LoadCustomSwiftSourceFilesPhase not found" }
            add(load + 1, LockstepSirPhase(run))
            val imports = indexOf(AddFoundationImportsPhase)
            check(imports >= 0) { "lockstep: unsupported SKIE version, phase AddFoundationImportsPhase not found" }
            add(imports + 1, LockstepProxyPhase(run))
        }
    }
}
