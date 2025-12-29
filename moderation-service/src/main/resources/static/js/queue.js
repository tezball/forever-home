/**
 * Queue UI JavaScript - HTMX enhancements and keyboard shortcuts
 */
(function() {
    'use strict';

    // Track selected card index for keyboard navigation
    let selectedIndex = -1;
    let queueCards = [];

    // Initialize on page load
    document.addEventListener('DOMContentLoaded', function() {
        initializeQueue();
        initializeKeyboardShortcuts();
    });

    // Re-initialize after HTMX swaps
    document.body.addEventListener('htmx:afterSwap', function(evt) {
        if (evt.detail.target.id === 'queue-list') {
            initializeQueue();
        }
    });

    /**
     * Initialize queue card interactions
     */
    function initializeQueue() {
        queueCards = document.querySelectorAll('.queue-card');

        queueCards.forEach((card, index) => {
            card.addEventListener('click', function() {
                selectCard(index);
            });
        });

        // Update indicator handling
        const updateIndicator = document.getElementById('update-indicator');
        if (updateIndicator) {
            document.body.addEventListener('htmx:beforeRequest', function(evt) {
                if (evt.detail.target && evt.detail.target.id === 'queue-list') {
                    updateIndicator.style.display = 'block';
                }
            });
            document.body.addEventListener('htmx:afterRequest', function(evt) {
                updateIndicator.style.display = 'none';
            });
        }
    }

    /**
     * Select a card by index
     */
    function selectCard(index) {
        if (index < 0 || index >= queueCards.length) return;

        // Remove selection from all cards
        queueCards.forEach(c => {
            c.classList.remove('ring-2', 'ring-primary');
        });

        // Select the new card
        selectedIndex = index;
        const card = queueCards[selectedIndex];
        card.classList.add('ring-2', 'ring-primary');

        // Scroll into view if needed
        card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    /**
     * Get the currently selected card
     */
    function getSelectedCard() {
        if (selectedIndex >= 0 && selectedIndex < queueCards.length) {
            return queueCards[selectedIndex];
        }
        return null;
    }

    /**
     * Initialize keyboard shortcuts
     */
    function initializeKeyboardShortcuts() {
        document.addEventListener('keydown', function(evt) {
            // Don't handle shortcuts when typing in inputs
            if (evt.target.tagName === 'INPUT' || evt.target.tagName === 'TEXTAREA' || evt.target.tagName === 'SELECT') {
                return;
            }

            switch (evt.key.toLowerCase()) {
                case 'j': // Navigate down
                    evt.preventDefault();
                    navigateDown();
                    break;
                case 'k': // Navigate up
                    evt.preventDefault();
                    navigateUp();
                    break;
                case 'a': // Approve selected
                    evt.preventDefault();
                    approveSelected();
                    break;
                case 'r': // Reject selected
                    evt.preventDefault();
                    rejectSelected();
                    break;
                case 's': // Skip to next
                    evt.preventDefault();
                    skipToNext();
                    break;
                case 'enter': // Open detail panel
                    evt.preventDefault();
                    openDetailPanel();
                    break;
                case '?': // Show help
                    if (evt.shiftKey) {
                        evt.preventDefault();
                        showKeyboardHelp();
                    }
                    break;
                case 'escape': // Clear selection
                    evt.preventDefault();
                    clearSelection();
                    break;
            }
        });
    }

    /**
     * Navigate to next card
     */
    function navigateDown() {
        if (queueCards.length === 0) return;

        if (selectedIndex < queueCards.length - 1) {
            selectCard(selectedIndex + 1);
        } else {
            selectCard(0); // Wrap to first
        }

        // Trigger detail panel load
        openDetailPanel();
    }

    /**
     * Navigate to previous card
     */
    function navigateUp() {
        if (queueCards.length === 0) return;

        if (selectedIndex > 0) {
            selectCard(selectedIndex - 1);
        } else {
            selectCard(queueCards.length - 1); // Wrap to last
        }

        // Trigger detail panel load
        openDetailPanel();
    }

    /**
     * Approve the selected item
     */
    function approveSelected() {
        const card = getSelectedCard();
        if (!card) {
            showNotification('No item selected. Use j/k to navigate.', 'warning');
            return;
        }

        const approveBtn = card.querySelector('button[type="submit"]:first-of-type, form[action*="approve"] button');
        if (approveBtn) {
            approveBtn.click();
            skipToNext();
        }
    }

    /**
     * Reject the selected item
     */
    function rejectSelected() {
        const card = getSelectedCard();
        if (!card) {
            showNotification('No item selected. Use j/k to navigate.', 'warning');
            return;
        }

        const rejectBtn = card.querySelector('form[action*="reject"] button');
        if (rejectBtn) {
            rejectBtn.click();
            skipToNext();
        }
    }

    /**
     * Skip to next item without action
     */
    function skipToNext() {
        navigateDown();
    }

    /**
     * Open detail panel for selected card
     */
    function openDetailPanel() {
        const card = getSelectedCard();
        if (card) {
            // Trigger HTMX request
            htmx.trigger(card, 'click');
        }
    }

    /**
     * Clear selection
     */
    function clearSelection() {
        queueCards.forEach(c => {
            c.classList.remove('ring-2', 'ring-primary');
        });
        selectedIndex = -1;

        // Clear detail panel
        const detailPanel = document.getElementById('detail-panel');
        if (detailPanel) {
            detailPanel.innerHTML = `
                <div class="p-8 text-center text-gray-500">
                    <svg class="w-16 h-16 mx-auto text-gray-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                    </svg>
                    <p>Click on an item to view details</p>
                </div>
            `;
        }
    }

    /**
     * Show keyboard shortcuts help
     */
    function showKeyboardHelp() {
        const helpHtml = `
            <div id="keyboard-help-modal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" onclick="this.remove()">
                <div class="bg-white rounded-lg shadow-xl p-6 max-w-md mx-4" onclick="event.stopPropagation()">
                    <div class="flex items-center justify-between mb-4">
                        <h3 class="text-lg font-semibold text-gray-900">Keyboard Shortcuts</h3>
                        <button onclick="document.getElementById('keyboard-help-modal').remove()" class="text-gray-400 hover:text-gray-600">
                            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                            </svg>
                        </button>
                    </div>
                    <div class="space-y-3">
                        <div class="flex justify-between">
                            <span class="text-gray-600">Navigate down</span>
                            <kbd class="px-2 py-1 bg-gray-100 rounded text-sm font-mono">j</kbd>
                        </div>
                        <div class="flex justify-between">
                            <span class="text-gray-600">Navigate up</span>
                            <kbd class="px-2 py-1 bg-gray-100 rounded text-sm font-mono">k</kbd>
                        </div>
                        <div class="flex justify-between">
                            <span class="text-gray-600">Approve selected</span>
                            <kbd class="px-2 py-1 bg-gray-100 rounded text-sm font-mono">a</kbd>
                        </div>
                        <div class="flex justify-between">
                            <span class="text-gray-600">Reject selected</span>
                            <kbd class="px-2 py-1 bg-gray-100 rounded text-sm font-mono">r</kbd>
                        </div>
                        <div class="flex justify-between">
                            <span class="text-gray-600">Skip to next</span>
                            <kbd class="px-2 py-1 bg-gray-100 rounded text-sm font-mono">s</kbd>
                        </div>
                        <div class="flex justify-between">
                            <span class="text-gray-600">Open details</span>
                            <kbd class="px-2 py-1 bg-gray-100 rounded text-sm font-mono">Enter</kbd>
                        </div>
                        <div class="flex justify-between">
                            <span class="text-gray-600">Clear selection</span>
                            <kbd class="px-2 py-1 bg-gray-100 rounded text-sm font-mono">Esc</kbd>
                        </div>
                        <div class="flex justify-between">
                            <span class="text-gray-600">Show this help</span>
                            <kbd class="px-2 py-1 bg-gray-100 rounded text-sm font-mono">?</kbd>
                        </div>
                    </div>
                </div>
            </div>
        `;

        // Remove existing modal if present
        const existing = document.getElementById('keyboard-help-modal');
        if (existing) {
            existing.remove();
        }

        document.body.insertAdjacentHTML('beforeend', helpHtml);
    }

    /**
     * Show a notification toast
     */
    function showNotification(message, type = 'info') {
        const colors = {
            'info': 'bg-blue-100 text-blue-800 border-blue-200',
            'success': 'bg-green-100 text-green-800 border-green-200',
            'warning': 'bg-amber-100 text-amber-800 border-amber-200',
            'error': 'bg-red-100 text-red-800 border-red-200'
        };

        const notification = document.createElement('div');
        notification.className = `fixed bottom-4 left-1/2 transform -translate-x-1/2 px-4 py-2 rounded-lg border ${colors[type]} shadow-lg z-50 animate-fade-in`;
        notification.textContent = message;

        document.body.appendChild(notification);

        setTimeout(() => {
            notification.classList.add('animate-fade-out');
            setTimeout(() => notification.remove(), 300);
        }, 2000);
    }

    // Add CSS animations
    const style = document.createElement('style');
    style.textContent = `
        @keyframes fadeIn {
            from { opacity: 0; transform: translate(-50%, 10px); }
            to { opacity: 1; transform: translate(-50%, 0); }
        }
        @keyframes fadeOut {
            from { opacity: 1; transform: translate(-50%, 0); }
            to { opacity: 0; transform: translate(-50%, 10px); }
        }
        .animate-fade-in {
            animation: fadeIn 0.3s ease-out forwards;
        }
        .animate-fade-out {
            animation: fadeOut 0.3s ease-out forwards;
        }
    `;
    document.head.appendChild(style);
})();
