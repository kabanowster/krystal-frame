package krystal.framework.core.flow.implementation;

import krystal.framework.core.flow.FlowInterface;

/**
 * Wrapper around flows, for which each the Phaser is created, within FlowControl initialization.
 */
public enum Flows implements FlowInterface {
	cache, flow, database, core
}