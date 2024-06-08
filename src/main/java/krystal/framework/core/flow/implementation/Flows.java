package krystal.framework.core.flow.implementation;

import krystal.framework.core.flow.FlowInterface;

/**
 * Wrapper around default flows, for which each the Phaser is created, within {@link FlowControl} initialization.
 */
public enum Flows implements FlowInterface {
	cache, flow, database, core
}